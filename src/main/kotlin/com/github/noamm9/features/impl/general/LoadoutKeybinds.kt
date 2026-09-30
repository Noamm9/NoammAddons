package com.github.noamm9.features.impl.general

import com.github.noamm9.config.types.KeybindSetting
import com.github.noamm9.config.types.ToggleSetting
//? if <26.2 {
/*import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
*///? } else {
import com.github.noamm9.event.impl.*
//? }
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
//? if >=26.2 {
import com.github.noamm9.utils.*
//? }
import com.github.noamm9.utils.ChatUtils.unformattedText
//? if <26.2 {
/*import com.github.noamm9.utils.GuiUtils
//? if cheat {
import com.github.noamm9.utils.ThreadUtils
//? }
import com.github.noamm9.utils.equalsOneOf
*///? }
import com.github.noamm9.utils.items.ItemUtils.lore
//? if <26.2 {
/*import gg.essential.universal.UKeyboard
*///? } else {
import com.mojang.blaze3d.platform.InputConstants
//? }
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
//? if <26.2 {
/*import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
*///? } else {
import net.minecraft.network.protocol.game.*
//? }
import net.minecraft.world.inventory.Slot
//? if <26.2 {
/*import org.lwjgl.glfw.GLFW
*///? }

object LoadoutKeybinds: Feature("Allows you to bind SkyBlock loadout slots to your keyboard.") {
    private val blockBarrierClick by ToggleSetting("Block Barrier Click")
    //? if cheat {
    private val closeAfterUse by ToggleSetting("Auto Close On Use")
    //? }
    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds")
    private val keybinds = (1 .. 12).mapIndexed { index, slot ->
        KeybindSetting("Loadout Slot $slot", when (index) {
            //? if <26.2 {
            /*in 0 .. 8 -> UKeyboard.KEY_1 + index
            9 -> UKeyboard.KEY_0
            10 -> UKeyboard.KEY_MINUS
            11 -> UKeyboard.KEY_EQUALS
            else -> UKeyboard.KEY_NONE
            *///? } else {
            in 0 .. 8 -> InputConstants.KEY_1 + index
            9 -> InputConstants.KEY_0
            10 -> InputConstants.KEY_MINUS
            11 -> InputConstants.KEY_EQUALS
            else -> InputConstants.UNKNOWN.value
            //? }
        }).hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    private val loadoutMenuRegex = Regex("""^\(\d+/\d+\) Loadouts$""")
    private var lastClick = System.currentTimeMillis()
    private var inLoadoutMenu = false
    //? if cheat {
    private var pendingAutoClose = false
    //? }
    private val slots = listOf(
        14, 15, 16,
        23, 24, 25,
        32, 33, 34,
        41, 42, 43
    )

    private val hotbarKeyMap by lazy {
        mc.options.keyHotbarSlots.withIndex().associate { (i, key) -> (key as IKeyMapping).key.value to i }
    }

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet is ClientboundOpenScreenPacket) inLoadoutMenu = event.packet.title.unformattedText.matches(loadoutMenuRegex)
            else if (event.packet is ClientboundContainerClosePacket && inLoadoutMenu) inLoadoutMenu = false
        }

        register<PacketEvent.Sent> {
            if (! inLoadoutMenu) return@register
            if (event.packet !is ServerboundContainerClosePacket) return@register
            inLoadoutMenu = false
            //? if cheat {
            pendingAutoClose = false
            //? }
        }

        //? if cheat {
        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.matches(loadoutMenuRegex)) return@register

            player.closeContainer()
            pendingAutoClose = false
        }
        //? }

        register<ContainerEvent.Keyboard> {
            if (! inLoadoutMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            //? if <26.2 {
            /*if (event.key.equalsOneOf(UKeyboard.KEY_ESCAPE, KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value)) return@register
            *///? } else {
            if (event.key.equalsOneOf(InputConstants.KEY_ESCAPE, KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value)) return@register
            //? }
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.key] ?: return@register
            else keybinds.indexOfFirst(KeybindSetting::isDown).takeUnless { it == - 1 } ?: return@register
            event.isCanceled = true
            click(index)
        }

        register<ContainerEvent.MouseClick> {
            if (! inLoadoutMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            //? if <26.2 {
            /*if (event.button.equalsOneOf(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return@register
            *///? } else {
            if (event.button.equalsOneOf(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT)) return@register
            //? }
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.button] ?: return@register
            else keybinds.indexOfFirst { it.matches(event.button, mouse = true) }.takeUnless { it == - 1 } ?: return@register
            event.isCanceled = true
            click(index)
        }

        register<ContainerEvent.SlotClick> {
            if (! blockBarrierClick.value) return@register
            if (! inLoadoutMenu) return@register
            if (event.slotId != 49) return@register
            event.isCanceled = true
        }
    }

    private fun click(index: Int) {
        val slot = slots[index].takeIf(::isSlotEquipable) ?: return
        GuiUtils.clickSlot(slot, GuiUtils.ButtonType.LEFT)

        lastClick = System.currentTimeMillis()
        //? if cheat {
        if (closeAfterUse.value) {
            player.closeContainer()
            ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
            pendingAutoClose = true
        }
        //? }
    }

    private fun isSlotEquipable(slot: Int) = player.containerMenu.getSlot(slot).takeIf(Slot::hasItem)?.item?.lore?.any { it.contains("Left-click to equip!") } ?: false
}