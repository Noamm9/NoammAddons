package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.mixin.IAbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ContainerInput

object GuiUtils {
    enum class ButtonType {
        LEFT, RIGHT, MIDDLE;
    }

    fun clickSlot(slotIndex: Int, btn: ButtonType) {
        val containerId = mc.player?.containerMenu?.containerId ?: return

        mc.gameMode?.handleContainerInput(
            containerId, slotIndex, btn.ordinal,
            if (btn == ButtonType.MIDDLE) ContainerInput.CLONE
            else ContainerInput.PICKUP, mc.player !!
        )
    }

    fun getSlotPos(screen: AbstractContainerScreen<*>, index: Int): Pair<Float, Float>? {
        val slot = screen.menu.slots.getOrNull(index) ?: return null
        val screenBase = screen as IAbstractContainerScreen
        return (screenBase.leftPos + slot.x).toFloat() to (screenBase.topPos + slot.y).toFloat()
    }
}