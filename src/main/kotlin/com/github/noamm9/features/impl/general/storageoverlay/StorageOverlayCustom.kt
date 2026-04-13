package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.customgui.CustomGui
import com.github.noamm9.ui.customgui.setSlotX
import com.github.noamm9.ui.customgui.setSlotY
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.world.inventory.Slot
import java.awt.Color

class StorageOverlayCustom(
    val handler: StorageBackingHandle,
    val screen: ContainerScreen,
    val overview: StorageOverlayScreen,
) : CustomGui() {

    override fun onVoluntaryExit(): Boolean {
        overview.isExiting = true
        StorageOverlayScreen.resetScroll()
        return super.onVoluntaryExit()
    }

    override fun onInit() {
        overview.init(mc, screen.width, screen.height)
        val accessor = screen as IAbstractContainerScreen
        accessor.setLeftPos(overview.measurements.x)
        accessor.setTopPos(overview.measurements.y)
        accessor.setImageWidth(overview.measurements.totalWidth)
        accessor.setImageHeight(overview.measurements.totalHeight)
    }

    override fun shouldDrawForeground(): Boolean = false

    override fun mouseClick(click: MouseButtonEvent, doubled: Boolean) = overview.mouseClicked(click, doubled, (handler as? StorageBackingHandle.Page)?.storagePageSlot)
    override fun mouseReleased(click: MouseButtonEvent) = overview.mouseReleased(click)
    override fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double) = overview.mouseDragged(click, deltaX, deltaY)
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double) = overview.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)

    override fun render(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.fill(0, 0, screen.width, screen.height, Color(0, 0, 0, 200).rgb)

        Render2D.drawRect(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(24, 24, 27))
        Render2D.drawBorder(context, overview.measurements.x, overview.measurements.y, overview.measurements.overviewWidth, overview.measurements.overviewHeight, Color(60, 60, 65))
        val activeSlot = (handler as? StorageBackingHandle.Page)?.storagePageSlot
        val chestSlots = screen.menu.slots.take(screen.menu.rowCount * 9).drop(9)
        overview.drawPages(context, mouseX, mouseY, delta, activeSlot, chestSlots)
        overview.drawScrollBar(context)
        overview.drawPlayerInventory(context, mouseX, mouseY)
    }

    override fun moveSlot(slot: Slot) {
        if (slot.container is net.minecraft.world.entity.player.Inventory) {
            val index = slot.containerSlot
            val (x, y) = overview.getPlayerInventorySlotPosition(index)
            val accessor = screen as IAbstractContainerScreen
            slot.setSlotX(x - accessor.leftPos)
            slot.setSlotY(y - accessor.topPos)
        } else {
            // Chest slot — hide (our overlay draws cached items, active page slots get repositioned by drawPage)
            slot.setSlotX(-100000)
            slot.setSlotY(-100000)
        }
    }

    override fun isClickOutsideBounds(mouseX: Double, mouseY: Double): Boolean = false
}
