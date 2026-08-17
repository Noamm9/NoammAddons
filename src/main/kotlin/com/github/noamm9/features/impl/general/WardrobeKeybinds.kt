package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.FCScheduler
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.equalsOneOf
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW

object WardrobeKeybinds: Feature("Make it possible to bind armor slots to your keyboard.") {
    private val wardrobeMenuRegex = Regex("""^\(\d+/\d+\) Armor Sets$""")
    private val fcScheduler = FCScheduler()
    private var inWardrobeMenu = false
    private var pendingAutoClose = false
    private val keyMap = mapOf(
        0 to 36, 1 to 37, 2 to 38, 3 to 39, 4 to 40,
        5 to 41, 6 to 42, 7 to 43, 8 to 44
    )

    private val hotbarKeyMap by lazy {
        mc.options.keyHotbarSlots.withIndex().associate { (i, key) -> (key as IKeyMapping).key.value to i }
    }

    private val closeAfterUse by ToggleSetting("Auto Close On Use")
    private val preventUnequip by ToggleSetting("Prevent Unequip")
    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds")
    private val keybinds = (1 .. 9).mapIndexed { index, slot ->
        KeybindSetting("Wardrobe Slot $slot", InputConstants.KEY_1 + index)
            .hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    override fun onDisable() {
        super.onDisable()
        reset()
    }

    override fun init() {
        register<WorldChangeEvent> { reset() }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet is ClientboundOpenScreenPacket) {
                inWardrobeMenu = event.packet.title.unformattedText.matches(wardrobeMenuRegex)
                if (inWardrobeMenu) fcScheduler.start() else fcScheduler.cancel()
            }
            else if (event.packet is ClientboundContainerClosePacket && inWardrobeMenu) {
                inWardrobeMenu = false
                fcScheduler.cancel()
            }
        }

        register<PacketEvent.Sent> {
            if (event.packet is ServerboundContainerClosePacket && inWardrobeMenu) {
                inWardrobeMenu = false
                fcScheduler.cancel()
                pendingAutoClose = false
            }
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.matches(wardrobeMenuRegex)) return@register

            pendingAutoClose = false
            player.closeContainer()
        }

        register<ContainerEvent.Keyboard> {
            if (! inWardrobeMenu) return@register
            if (event.key.equalsOneOf(GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_E)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.key] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            event.isCanceled = queueKeybind(index)
        }

        register<ContainerEvent.MouseClick> {
            if (! inWardrobeMenu) return@register
            if (event.button.equalsOneOf(0, 1, 2)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.button] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            event.isCanceled = queueKeybind(index)
        }
    }

    private fun queueKeybind(index: Int): Boolean {
        val slot = getSlot(index) ?: return false
        if (! preventUnequip.value || ! isSlotEquipped(slot)) {
            fcScheduler.runOrQueue { if (enabled && inWardrobeMenu) handleKeybind(index) }
        }
        return true
    }

    private fun handleKeybind(index: Int) {
        val slot = getSlot(index) ?: return
        if (isSlotEquipped(slot) && preventUnequip.value) return

        GuiUtils.clickSlot(slot, GuiUtils.ButtonType.LEFT)
        if (closeAfterUse.value) closeAfterReopen()
    }

    private fun getSlot(index: Int) = keyMap[index]?.takeUnless { player.containerMenu.getSlot(it).item == ItemStack.EMPTY }

    private fun isSlotEquipped(slot: Int) = player.containerMenu.slots[slot].item.`is`(Items.LIME_DYE)

    fun closeAfterReopen() {
        player.closeContainer()
        ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
        pendingAutoClose = true
    }

    private fun reset() {
        inWardrobeMenu = false
        pendingAutoClose = false
        fcScheduler.cancel()
    }
}