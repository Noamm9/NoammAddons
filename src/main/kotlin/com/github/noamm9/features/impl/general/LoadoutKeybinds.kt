package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.lore
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

object LoadoutKeybinds: Feature("Allows you to bind SkyBlock loadout slots to your keyboard.") {
    private val loadoutMenuRegex = Regex("""^\(\d+/\d+\) Loadouts$""", RegexOption.IGNORE_CASE)
    private var lastClick = System.currentTimeMillis()
    private var inLoadoutMenu = false
    private var pendingAutoClose = false
    private val keyMap = mapOf(
        0 to 14, 1 to 15, 2 to 16,
        3 to 23, 4 to 24, 5 to 25,
        6 to 32, 7 to 33, 8 to 34,
        9 to 41, 10 to 42, 11 to 43
    )

    private val hotbarKeyMap by lazy {
        mc.options.keyHotbarSlots.withIndex().associate { (i, key) -> (key as IKeyMapping).key.value to i }
    }

    private val closeAfterUse by ToggleSetting("Auto Close On Use")
    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds")
    private val keybinds = (1 .. 12).mapIndexed { index, slot ->
        val defaultKey = if (index < 9) InputConstants.KEY_1 + index else GLFW.GLFW_KEY_UNKNOWN
        KeybindSetting("Loadout Slot $slot", defaultKey)
            .hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet is ClientboundOpenScreenPacket) {
                inLoadoutMenu = event.packet.title.unformattedText.matches(loadoutMenuRegex)
            }
            else if (event.packet is ClientboundContainerClosePacket && inLoadoutMenu) {
                inLoadoutMenu = false
            }
        }

        register<PacketEvent.Sent> {
            if (event.packet !is ServerboundContainerClosePacket || ! inLoadoutMenu) return@register
            inLoadoutMenu = false
            pendingAutoClose = false
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.matches(loadoutMenuRegex)) return@register

            pendingAutoClose = false
            mc.player?.closeContainer()
        }

        register<ContainerEvent.Keyboard> {
            if (! inLoadoutMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            if (event.key.equalsOneOf(GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_E)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.key] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            event.isCanceled = true
            handleKeybind(index)
        }

        register<ContainerEvent.MouseClick> {
            if (! inLoadoutMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            if (event.button.equalsOneOf(0, 1, 2)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.button] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            event.isCanceled = true
            handleKeybind(index)
        }
    }

    private fun handleKeybind(index: Int) {
        val slot = keyMap[index]?.takeUnless { mc.player !!.containerMenu.getSlot(it).item == ItemStack.EMPTY } ?: return

        if (! isSlotEquipable(slot)) return

        GuiUtils.clickSlot(slot, GuiUtils.ButtonType.LEFT)

        lastClick = System.currentTimeMillis()
        if (closeAfterUse.value) closeAfterReopen()
    }

    private fun isSlotEquipable(slot: Int): Boolean {
        return mc.player?.containerMenu?.slots?.get(slot)?.item?.lore?.any {
            it.contains("Left-click to equip!", ignoreCase = true)
        } ?: false
    }

    fun closeAfterReopen() {
        mc.player?.closeContainer()
        ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
        pendingAutoClose = true
    }
}
