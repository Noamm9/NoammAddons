package com.github.noamm9.features.impl.general

import com.github.noamm9.config.types.KeybindSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemUtils.lore
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.network.protocol.game.*
import net.minecraft.world.inventory.Slot

object LoadoutKeybinds: Feature("Allows you to bind SkyBlock loadout slots to your keyboard.") {
    private val blockBarrierClick by ToggleSetting("Block Barrier Click")
    //#if CHEAT
    private val closeAfterUse by ToggleSetting("Auto Close On Use")
    //#endif
    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds")
    private val keybinds = (1 .. 12).mapIndexed { index, slot ->
        KeybindSetting("Loadout Slot $slot", when (index) {
            in 0 .. 8 -> InputConstants.KEY_1 + index
            9 -> InputConstants.KEY_0
            10 -> InputConstants.KEY_MINUS
            11 -> InputConstants.KEY_EQUALS
            else -> InputConstants.UNKNOWN.value
        }).hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    private val loadoutMenuRegex = Regex("""^\(\d+/\d+\) Loadouts$""")
    private var lastClick = System.currentTimeMillis()
    private var inLoadoutMenu = false
    //#if CHEAT
    private var pendingAutoClose = false
    //#endif
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
            //#if CHEAT
            pendingAutoClose = false
            //#endif
        }

        //#if CHEAT
        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.matches(loadoutMenuRegex)) return@register

            player.closeContainer()
            pendingAutoClose = false
        }
        //#endif

        register<ContainerEvent.Keyboard> {
            if (! inLoadoutMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            if (event.key.equalsOneOf(InputConstants.KEY_ESCAPE, KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.key] ?: return@register
            else keybinds.indexOfFirst(KeybindSetting::isDown).takeUnless { it == - 1 } ?: return@register
            event.isCanceled = true
            click(index)
        }

        register<ContainerEvent.MouseClick> {
            if (! inLoadoutMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            if (event.button.equalsOneOf(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT)) return@register
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
        //#if CHEAT
        if (closeAfterUse.value) {
            player.closeContainer()
            ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
            pendingAutoClose = true
        }
        //#endif
    }

    private fun isSlotEquipable(slot: Int) = player.containerMenu.getSlot(slot).takeIf(Slot::hasItem)?.item?.lore?.any { it.contains("Left-click to equip!") } ?: false
}