package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.customgui.CustomGui
import com.github.noamm9.ui.customgui.setSlotX
import com.github.noamm9.ui.customgui.setSlotY
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import java.awt.Color

internal class StorageOverlayCustom(val handler: StorageBackingHandle, val screen: ContainerScreen, val overview: StorageOverlayScreen): CustomGui() {
    override fun onVoluntaryExit(): Boolean {
        overview.isExiting = true
        StorageOverlayScreen.resetScroll()
        return super.onVoluntaryExit()
    }

    override fun onInit() {
        overview.init(NoammAddons.mc, screen.width, screen.height)
        val accessor = screen as IAbstractContainerScreen
        accessor.setLeftPos(overview.measurements.x)
        accessor.setTopPos(overview.measurements.y)
        accessor.setImageWidth(overview.measurements.overviewWidth)
        accessor.setImageHeight(overview.measurements.totalHeight)
    }

    override fun shouldDrawForeground() = false

    override fun mouseClick(click: MouseButtonEvent, doubled: Boolean) = overview.mouseClicked(click, doubled, (handler as? StorageBackingHandle.Page)?.storagePageSlot)
    override fun mouseReleased(click: MouseButtonEvent) = overview.mouseReleased(click)
    override fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double) = overview.mouseDragged(click, deltaX, deltaY)
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double) = overview.mouseScrolled(mouseX, mouseY, verticalAmount, (handler as? StorageBackingHandle.Page)?.storagePageSlot)

    override fun render(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        Render2D.drawRect(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(24, 24, 27))
        Render2D.drawBorder(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(60, 60, 65))
        val activeSlot = (handler as? StorageBackingHandle.Page)?.storagePageSlot
        val chestSlots = screen.menu.slots.take(screen.menu.rowCount * 9).drop(9)
        overview.drawPages(context, mouseX, mouseY, delta, activeSlot, chestSlots)
        overview.drawScrollBar(context)
        overview.drawPlayerInventory(context, mouseX, mouseY)
    }

    override fun moveSlot(slot: Slot) {
        if (slot.container is Inventory) {
            val (x, y) = overview.getPlayerInventorySlotPosition(slot.containerSlot)
            val accessor = screen as IAbstractContainerScreen
            slot.setSlotX(x - accessor.leftPos)
            slot.setSlotY(y - accessor.topPos)
        }
        else {
            slot.setSlotX(- 100000)
            slot.setSlotY(- 100000)
        }
    }

    override fun isClickOutsideBounds(mouseX: Double, mouseY: Double) = false
}