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
import com.github.noamm9.utils.items.ItemUtils.lore
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import org.lwjgl.glfw.GLFW

object LoadoutKeybinds: Feature("Allows you to bind SkyBlock loadout slots to your keyboard.") {
    private val closeAfterUse by ToggleSetting("Auto Close On Use")
    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds")
    private val keybinds = (1 .. 12).mapIndexed { index, slot ->
        KeybindSetting("Loadout Slot $slot", when (index) {
            in 0 .. 8 -> InputConstants.KEY_1 + index
            9 -> GLFW.GLFW_KEY_0
            10 -> GLFW.GLFW_KEY_MINUS
            11 -> GLFW.GLFW_KEY_EQUAL
            else -> GLFW.GLFW_KEY_UNKNOWN
        }).hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    private val loadoutMenuRegex = Regex("""^\(\d+/\d+\) Loadouts$""")
    private val fcScheduler = FCScheduler()
    private var inLoadoutMenu = false
    private var pendingAutoClose = false
    private val slots = listOf(
        14, 15, 16,
        23, 24, 25,
        32, 33, 34,
        41, 42, 43
    )

    private val hotbarKeyMap by lazy {
        mc.options.keyHotbarSlots.withIndex().associate { (i, key) -> (key as IKeyMapping).key.value to i }
    }

    override fun onDisable() {
        super.onDisable()
        reset()
    }

    override fun init() {
        register<WorldChangeEvent> { reset() }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet is ClientboundOpenScreenPacket) {
                inLoadoutMenu = event.packet.title.unformattedText.matches(loadoutMenuRegex)
                if (inLoadoutMenu) fcScheduler.start() else fcScheduler.cancel()
            }
            else if (event.packet is ClientboundContainerClosePacket && inLoadoutMenu) {
                inLoadoutMenu = false
                fcScheduler.cancel()
            }
        }

        register<PacketEvent.Sent> {
            if (! inLoadoutMenu) return@register
            if (event.packet !is ServerboundContainerClosePacket) return@register
            inLoadoutMenu = false
            fcScheduler.cancel()
            pendingAutoClose = false
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.matches(loadoutMenuRegex)) return@register

            pendingAutoClose = false
            player.closeContainer()
        }

        register<ContainerEvent.Keyboard> {
            if (! inLoadoutMenu) return@register
            if (event.key.equalsOneOf(GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_E)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.key] ?: return@register
            else keybinds.indexOfFirst(KeybindSetting::isDown).takeUnless { it == - 1 } ?: return@register
            event.isCanceled = true
            queueKeybind(index)
        }

        register<ContainerEvent.MouseClick> {
            if (! inLoadoutMenu) return@register
            if (event.button.equalsOneOf(0, 1, 2)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.button] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            event.isCanceled = true
            queueKeybind(index)
        }
    }

    private fun queueKeybind(index: Int) {
        if (! isSlotEquipable(slots[index])) return
        fcScheduler.runOrQueue { if (enabled && inLoadoutMenu) handleKeybind(index) }
    }

    private fun handleKeybind(index: Int) {
        val slot = slots[index].takeIf(::isSlotEquipable) ?: return
        GuiUtils.clickSlot(slot, GuiUtils.ButtonType.LEFT)

        if (closeAfterUse.value) {
            player.closeContainer()
            ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
            pendingAutoClose = true
        }
    }

    private fun isSlotEquipable(slot: Int) = player.containerMenu.getSlot(slot).item.lore.any { it.contains("Left-click to equip!") }

    private fun reset() {
        inLoadoutMenu = false
        pendingAutoClose = false
        fcScheduler.cancel()
    }
}
