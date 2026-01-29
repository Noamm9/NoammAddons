package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.hideIf
import com.github.noamm9.ui.clickgui.componnents.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.ButtonType
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW


object WardrobeKeybinds: Feature("Make it possible to bind armor slots to your keyboard") {
    private val wardrobeMenuRegex = Regex("^Wardrobe \\(\\d/\\d\\)$")
    private var lastClick = System.currentTimeMillis()
    private var inWardrobeMenu = false
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
            }
        }

        register<ContainerEvent.Keyboard> {
            if (! inWardrobeMenu) return@register
            if (System.currentTimeMillis() - lastClick < 300) return@register
            if (event.key.equalsOneOf(GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_E)) return@register
            val index = if (useHotbarBinds.value) hotbarKeyMap[event.key] ?: return@register
            else keybinds.withIndex().find { (_, key) -> key.isDown() }?.index ?: return@register
            val slot = keyMap[index]?.takeUnless { mc.player !!.containerMenu.getSlot(it).item == ItemStack.EMPTY } ?: return@register
            event.isCanceled = true

            if (isSlotEquipped(slot) && preventUnequip.value) return@register

            GuiUtils.clickSlot(slot, ButtonType.LEFT)

            lastClick = System.currentTimeMillis()
            if (closeAfterUse.value) mc.player !!.closeContainer()
        }
    }

    private fun isSlotEquipped(slot: Int): Boolean {
        return mc.player?.containerMenu?.slots?.get(slot)?.item?.`is`(Items.LIME_DYE) ?: false
    }
}