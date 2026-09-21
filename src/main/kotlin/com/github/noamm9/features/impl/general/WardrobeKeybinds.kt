package com.github.noamm9.features.impl.general

//#if CHEAT
//#endif
import com.github.noamm9.config.types.KeybindSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object WardrobeKeybinds: Feature("Make it possible to bind armor slots to your keyboard.") {
    //#if CHEAT
    private val closeAfterUse by ToggleSetting("Auto Close On Use")

    //#endif
    private val preventUnequip by ToggleSetting("Prevent Unequip")
    private val useHotbarBinds by ToggleSetting("Use Hotbar Binds")
    private val keybinds = (1 .. 9).mapIndexed { index, slot ->
        KeybindSetting("Wardrobe Slot $slot", InputConstants.KEY_1 + index)
            .hideIf { useHotbarBinds.value }.apply(configSettings::add)
    }

    private val wardrobeMenuRegex = Regex("""^\(\d+/\d+\) Armor Sets$""")
    private var lastClick = System.currentTimeMillis()
    private var inWardrobeMenu = false

    //#if CHEAT
    private var pendingAutoClose = false

    //#endif
    private val keyMap = listOf(36, 37, 38, 39, 40, 41, 42, 43, 44)

    private val hotbarKeyMap by lazy {
        mc.options.keyHotbarSlots.withIndex().associate { (i, key) -> (key as IKeyMapping).key.value to i }
    }

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet is ClientboundOpenScreenPacket) {
                inWardrobeMenu = event.packet.title.unformattedText.matches(wardrobeMenuRegex)
            }
            else if (event.packet is ClientboundContainerClosePacket && inWardrobeMenu) {
                inWardrobeMenu = false
            }
        }

        register<PacketEvent.Sent> {
            if (event.packet is ServerboundContainerClosePacket && inWardrobeMenu) {
                inWardrobeMenu = false
                //#if CHEAT
                pendingAutoClose = false
                //#endif
            }
        }

        //#if CHEAT
        register<MainThreadPacketReceivedEvent.Post> {
            if (! pendingAutoClose) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.matches(wardrobeMenuRegex)) return@register

            pendingAutoClose = false
            player.closeContainer()
        }
        //#endif

        register<ContainerEvent.Keyboard> {
            if (! inWardrobeMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            if (event.key.equalsOneOf(InputConstants.KEY_ESCAPE, KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.key] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            val slot = keyMap.getOrNull(index)?.takeUnless { player.containerMenu.getSlot(it).item == ItemStack.EMPTY } ?: return@register
            event.isCanceled = true

            if (isSlotEquipped(slot) && preventUnequip.value) return@register

            GuiUtils.clickSlot(slot, GuiUtils.ButtonType.LEFT)

            lastClick = System.currentTimeMillis()
            //#if CHEAT
            if (closeAfterUse.value) closeAfterReopen()
            //#endif
        }

        register<ContainerEvent.MouseClick> {
            if (! inWardrobeMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            if (event.button.equalsOneOf(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.MOUSE_BUTTON_MIDDLE)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.button] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            val slot = keyMap.getOrNull(index)?.takeUnless { player.containerMenu.getSlot(it).item == ItemStack.EMPTY } ?: return@register
            event.isCanceled = true

            if (isSlotEquipped(slot) && preventUnequip.value) return@register

            GuiUtils.clickSlot(slot, GuiUtils.ButtonType.LEFT)

            lastClick = System.currentTimeMillis()
            //#if CHEAT
            if (closeAfterUse.value) closeAfterReopen()
            //#endif
        }
    }

    private fun isSlotEquipped(slot: Int) = player.containerMenu.slots[slot].item.`is`(Items.DYE.lime())

    //#if CHEAT
    fun closeAfterReopen() {
        player.closeContainer()
        ThreadUtils.setTimeout(3000) { pendingAutoClose = false }
        pendingAutoClose = true
    }
    //#endif
}