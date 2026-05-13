package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.inventory.Slot
import java.awt.Color

internal class StorageOverlayCustom(val handler: StorageBackingHandle, val screen: ContainerScreen, val overview: StorageOverlayScreen) {
    private fun syncContainerBounds() {
        overview.init(NoammAddons.mc, Resolution.width.toInt(), Resolution.height.toInt())
        val accessor = screen as IAbstractContainerScreen
        accessor.setLeftPos(0)
        accessor.setTopPos(0)
        accessor.setImageWidth(screen.width)
        accessor.setImageHeight(screen.height)
    }

    fun onVoluntaryExit() {
        overview.isExiting = true
        StorageOverlayScreen.resetScroll()
    }

    fun onInit() {
        syncContainerBounds()
    }

    fun mouseClick(click: MouseButtonEvent, doubled: Boolean) = overview.mouseClicked(click.x(), click.y(), click.button(), click.modifiers(), (handler as? StorageBackingHandle.Page)?.storagePageSlot)
    fun mouseReleased(click: MouseButtonEvent) = overview.mouseReleased()
    fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double) = overview.mouseDragged(click.y())
    fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double) = overview.mouseScrolled(
        Resolution.getMouseX(mouseX).toDouble(),
        Resolution.getMouseY(mouseY).toDouble(),
        verticalAmount,
        (handler as? StorageBackingHandle.Page)?.storagePageSlot
    )

    fun render(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        Resolution.refresh()
        syncContainerBounds()
        Resolution.push(context)
        val scaledMouseX = Resolution.getMouseX(mouseX.toDouble())
        val scaledMouseY = Resolution.getMouseY(mouseY.toDouble())
        Render2D.drawRect(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(24, 24, 27))
        Render2D.drawBorder(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(60, 60, 65))
        val activeSlot = (handler as? StorageBackingHandle.Page)?.storagePageSlot
        val chestSlots = screen.menu.slots.take(screen.menu.rowCount * 9).drop(9)
        overview.drawPages(context, scaledMouseX, scaledMouseY, delta, activeSlot, chestSlots)
        overview.drawScrollBar(context)
        overview.drawPlayerInventory(context, scaledMouseX, scaledMouseY)
        Resolution.pop(context)
    }

    fun renderCarriedItem(context: GuiGraphics, mouseX: Int, mouseY: Int): Boolean {
        val carried = screen.menu.carried
        if (carried.isEmpty) return true

        Resolution.refresh()
        Resolution.push(context)
        val scaledMouseX = Resolution.getMouseX(mouseX.toDouble()) - 8
        val scaledMouseY = Resolution.getMouseY(mouseY.toDouble()) - 8
        context.renderItem(carried, scaledMouseX, scaledMouseY)
        context.renderItemDecorations(screen.font, carried, scaledMouseX, scaledMouseY)
        Resolution.pop(context)
        return true
    }

    fun moveSlot(slot: Slot) {
        (slot as ICoordRememberingSlot).noammaddons_setX(- 100000)
        (slot as ICoordRememberingSlot).noammaddons_setY(- 100000)
    }

    fun isPointOverSlot(slot: Slot, xO: Int, yO: Int, pX: Double, pY: Double) = pX >= slot.x + xO && pX < slot.x + xO + 16 && pY >= slot.y + yO && pY < slot.y + yO + 16
}
