@file:Suppress("NOTHING_TO_INLINE")

package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.misc.InventorySearch
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.roundToInt

private inline fun inRect(mx: Double, my: Double, x: Int, y: Int, w: Int, h: Int) = mx >= x && mx < x + w && my >= y && my < y + h
private inline fun inRect(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int) = mx >= x && mx < x + w && my >= y && my < y + h

internal class StorageOverlayScreen: Screen(Component.literal("Storage Overlay")) {
    companion object {
        const val SLOT_SIZE = 17 /// 17x17 instead of 16x16 because the border thickness is 1
        const val PADDING = 10
        const val PAGE_WIDTH = SLOT_SIZE * 9 + 4
        const val SCROLL_BAR_WIDTH = 8
        const val SCROLL_BAR_HEIGHT = 16
        const val PLAYER_WIDTH = SLOT_SIZE * 9 + 6
        const val PLAYER_HEIGHT = SLOT_SIZE * 4 + 18

        var scroll: Float = 0f
        var lastRenderedInnerHeight = 0

        fun resetScroll() {
            if (! StorageOverlay.retainScrollSetting.value) scroll = 0f
        }
    }

    private val panelColor = Color(24, 24, 27, 220)
    private val slotBgColor = Color(50, 50, 55, 200)
    private val slotCellBg = Color(30, 30, 34).rgb
    private val slotCellBorder = Color(55, 55, 60).rgb
    private val activePageBorder = Color(80, 200, 80)
    private val scrollBgColor = Color(30, 30, 35, 180)
    private val scrollKnobColor = Color(120, 120, 130)
    private val borderColor = Color(60, 60, 65)

    var isExiting = false
    private var pageWidthCount = StorageOverlay.columnsSetting.value
    private var knobGrabbed = false

    var handler: StorageBackingHandle? = null
    var containerScreen: ContainerScreen? = null

    inner class Measurements {
        val innerScrollPanelWidth = PAGE_WIDTH * pageWidthCount + (pageWidthCount - 1) * PADDING
        val overviewWidth = innerScrollPanelWidth + 3 * PADDING + SCROLL_BAR_WIDTH
        val x = width / 2 - overviewWidth / 2
        val overviewHeight = minOf(height - PLAYER_HEIGHT - minOf(80, height / 10), StorageOverlay.maxHeightSetting.value)
        val innerScrollPanelHeight = overviewHeight - PADDING * 2
        val y = height / 2 - (overviewHeight + PLAYER_HEIGHT) / 2
        val playerX = width / 2 - PLAYER_WIDTH / 2
        val playerY = y + overviewHeight + 2
    }

    private var measurements = Measurements()

    private val scrollPanelX get() = measurements.x + PADDING
    private val scrollPanelY get() = measurements.y + PADDING
    private val scrollPanelW get() = measurements.innerScrollPanelWidth
    private val scrollPanelH get() = measurements.innerScrollPanelHeight

    private val scrollBarX get() = measurements.x + PADDING + measurements.innerScrollPanelWidth + PADDING
    private val scrollBarY get() = measurements.y + PADDING
    private val scrollBarH get() = measurements.innerScrollPanelHeight
    private val maxScroll get() = (lastRenderedInnerHeight.toFloat() + 6 - measurements.innerScrollPanelHeight).coerceAtLeast(0f)

    override fun init() {
        super.init()
        Resolution.refresh()
        val oldMax = maxScroll
        val scrollPct = if (oldMax > 0) scroll / oldMax else 0f
        pageWidthCount = StorageOverlay.columnsSetting.value.coerceAtMost((width - PADDING) / (PAGE_WIDTH + PADDING)).coerceAtLeast(1)
        measurements = Measurements()
        val newMax = maxScroll
        scroll = (scrollPct * newMax).coerceAtMost(newMax).coerceAtLeast(0f)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double) = mouseScrolled(mouseX, mouseY, verticalAmount, null)
    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean) = mouseClicked(click.x(), click.y(), click.button(), click.modifiers(), null)
    override fun mouseReleased(click: MouseButtonEvent) = mouseReleased()
    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double) = mouseDragged(click.y())

    override fun onClose() {
        isExiting = true
        resetScroll()
        super.onClose()
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        Resolution.refresh()
        val resolutionWidth = Resolution.width.roundToInt()
        val resolutionHeight = Resolution.height.roundToInt()
        if (width != resolutionWidth || height != resolutionHeight) {
            init(mc, resolutionWidth, resolutionHeight)
        }

        val mx = Resolution.getMouseX(mouseX.toDouble())
        val my = Resolution.getMouseY(mouseY.toDouble())

        Resolution.push(context)
        Render2D.drawRect(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, panelColor)
        Render2D.drawBorder(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, borderColor)
        context.drawPages(mx, my, null, null)
        context.drawScrollBar()
        context.drawPlayerInventory(mx, my)
        Resolution.pop(context)
    }

    private fun GuiGraphics.drawPages(mouseX: Int, mouseY: Int, excluding: StoragePageSlot?, slots: List<Slot>?) {
        enableScissor(scrollPanelX, scrollPanelY, scrollPanelX + scrollPanelW, scrollPanelY + scrollPanelH)
        val data = StorageOverlay.storageData
        val viewTop = scrollPanelY
        val viewBottom = scrollPanelY + scrollPanelH
        layoutedForEach(data) { x, y, _, ph, page, inventory ->
            if (y + ph < viewTop || y > viewBottom) return@layoutedForEach
            drawPage(x, y, inventory, if (excluding == page) slots else null, mouseX, mouseY)
        }
        disableScissor()
    }

    private fun GuiGraphics.drawScrollBar() {
        Render2D.drawRect(this, scrollBarX, scrollBarY, SCROLL_BAR_WIDTH, scrollBarH, scrollBgColor)
        val maxScroll = maxScroll
        val percentage = if (maxScroll > 0) scroll / maxScroll else 0f
        val knobY = scrollBarY + (percentage * (scrollBarH - SCROLL_BAR_HEIGHT)).toInt()
        Render2D.drawRect(this, scrollBarX, knobY, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, scrollKnobColor)
    }

    private fun getPlayerInvSlotPos(index: Int): Pair<Int, Int> {
        val slotsWidth = 9 * SLOT_SIZE
        val baseX = measurements.playerX + (PLAYER_WIDTH - slotsWidth) / 2 - SLOT_SIZE / 2 + 1
        val baseY = measurements.playerY + 8
        if (index < 9) {
            return Pair(baseX + index * SLOT_SIZE, baseY + 3 * SLOT_SIZE + 4)
        }
        return Pair(baseX + (index % 9) * SLOT_SIZE, baseY + (index / 9 - 1) * SLOT_SIZE)
    }

    private fun getPlayerInvIndex(mouseX: Int, mouseY: Int): Int? {
        for (index in 0 until 36) {
            val (slotX, slotY) = getPlayerInvSlotPos(index)
            if (inRect(mouseX, mouseY, slotX, slotY, 17, 17)) return index
        }
        return null
    }

    private fun GuiGraphics.drawSlotGrid(x: Int, y: Int, rows: Int) {
        val w = 9 * SLOT_SIZE
        val h = rows * SLOT_SIZE
        fill(x, y, x + w, y + h, slotCellBg)
        for (col in 0 .. 9) {
            val lx = x + col * SLOT_SIZE
            fill(lx, y, lx + 1, y + h, slotCellBorder)
        }
        for (row in 0 .. rows) {
            val ly = y + row * SLOT_SIZE
            fill(x, ly, x + w, ly + 1, slotCellBorder)
        }
    }

    private fun GuiGraphics.drawPlayerInventory(mouseX: Int, mouseY: Int) {
        val items = mc.player?.inventory?.nonEquipmentItems ?: return
        val (invX, invY) = getPlayerInvSlotPos(9)
        val (hotX, hotY) = getPlayerInvSlotPos(0)
        var hoveredStack: ItemStack? = null

        drawSlotGrid(invX - 1, invY - 1, 3)
        drawSlotGrid(hotX - 1, hotY - 1, 1)

        for (i in 0 until 36) {
            val item = items[i]
            val (sx, sy) = getPlayerInvSlotPos(i)
            val isSlotHovered = inRect(mouseX, mouseY, sx, sy, 16, 16)

            if (! item.isEmpty) {
                if (InventorySearch.matches(item)) {
                    Render2D.drawRect(this, sx, sy, 16, 16, InventorySearch.color)
                }

                renderItem(item, sx, sy, 0)
                renderItemDecorations(font, item, sx, sy)

                if (hoveredStack == null && isSlotHovered) {
                    hoveredStack = item
                }
            }

            if (isSlotHovered) Render2D.drawRect(this, sx, sy, 16, 16, Color.white.withAlpha(50))
        }

        if (hoveredStack != null) {
            setTooltipForNextFrame(font, hoveredStack, Resolution.toGuiScaled(mouseX), Resolution.toGuiScaled(mouseY))
        }
    }

    private fun GuiGraphics.drawPage(x: Int, y: Int, inventory: StorageData.StorageInventory, slots: List<Slot>?, mouseX: Int, mouseY: Int): Int {
        val inv = inventory.inventory
        if (inv == null && slots == null) {
            Render2D.drawRect(this, x, y, PAGE_WIDTH, 18, slotBgColor)
            Render2D.drawBorder(this, x, y, PAGE_WIDTH, 18, borderColor)
            Render2D.drawString(this, inventory.title + " - Click to load", x + 4f, y + 5f, Color(180, 180, 180))
            return 18
        }
        val rows = inv?.rows ?: (slots?.size?.div(9)?.coerceIn(1, 5) ?: 3)

        val name = inventory.title
        val isActive = slots != null
        val slotsY = y + 5 + font.lineHeight
        val pageHeight = rows * SLOT_SIZE + 8 + font.lineHeight

        if (isActive) {
            Render2D.drawBorder(this, x, y, PAGE_WIDTH + 1, pageHeight, activePageBorder, 2)
        }

        drawString(font, Component.literal(name), x + 6, y + 3, if (isActive) activePageBorder.rgb else 0xFFFFFF, true)

        val panelX = scrollPanelX
        val panelY = scrollPanelY
        val panelW = scrollPanelW
        val panelH = scrollPanelH
        var hoveredStack: ItemStack? = null
        var hoveredX = 0
        var hoveredY = 0

        drawSlotGrid(x + 2, slotsY, rows)

        val invStacks = inv?.stacks
        val itemCount = invStacks?.size ?: (slots?.size ?: (rows * 9))

        for (index in 0 until itemCount) {
            val slotX = (index % 9) * SLOT_SIZE + x + 3
            val slotY = (index / 9) * SLOT_SIZE + slotsY + 1

            if (slotY + 16 < panelY || slotY > panelY + panelH) continue
            val displayStack = if (slots != null && index < slots.size) slots[index].item else invStacks?.get(index) ?: continue
            val isSlotHovered = inRect(mouseX, mouseY, slotX, slotY, 16, 16) && inRect(mouseX, mouseY, panelX, panelY, panelW, panelH)

            if (! displayStack.isEmpty) {
                if (InventorySearch.matches(displayStack)) {
                    Render2D.drawRect(this, slotX, slotY, 16, 16, InventorySearch.color)
                }

                renderItem(displayStack, slotX, slotY)
                renderItemDecorations(font, displayStack, slotX, slotY)

                if (isSlotHovered && hoveredStack == null) {
                    hoveredStack = displayStack
                    hoveredX = mouseX
                    hoveredY = mouseY
                }
            }

            if (isSlotHovered) Render2D.drawRect(this, slotX, slotY, 16, 16, Color.white.withAlpha(50))
        }

        if (hoveredStack != null) {
            setTooltipForNextFrame(font, hoveredStack, Resolution.toGuiScaled(hoveredX), Resolution.toGuiScaled(hoveredY))
        }

        return pageHeight + 6
    }

    private inline fun layoutedForEach(data: StorageData, func: (x: Int, y: Int, pageWidth: Int, pageHeight: Int, page: StoragePageSlot, inventory: StorageData.StorageInventory) -> Unit) {
        var yOffset = - scroll.toInt()
        var xOffset = 0
        var maxHeight = 0
        for ((page, inventory) in data.storageInventories.entries) {
            val currentHeight = inventory.inventory?.let { it.rows * SLOT_SIZE + 6 + font.lineHeight } ?: 18
            maxHeight = maxOf(maxHeight, currentHeight)
            val rectX = measurements.x + PADDING + (PAGE_WIDTH + PADDING) * xOffset
            val rectY = yOffset + measurements.y + PADDING
            func(rectX, rectY, PAGE_WIDTH, currentHeight, page, inventory)
            xOffset ++
            if (xOffset >= pageWidthCount) {
                yOffset += maxHeight
                xOffset = 0
                maxHeight = 0
            }
        }
        lastRenderedInnerHeight = maxHeight + yOffset + scroll.toInt()
    }

    private fun dispatchActivePageSlotClick(button: Int, modifiers: Int, mouseX: Double, mouseY: Double, activePage: StoragePageSlot): Boolean {
        val containerScreen = mc.screen as? AbstractContainerScreen<*> ?: return false
        val menu = containerScreen.menu
        val chestSlots = menu.slots.take(menu.slots.size - 36).drop(9)
        if (chestSlots.isEmpty()) return false

        var hit = - 1
        val data = StorageOverlay.storageData
        layoutedForEach(data) { x, y, _, _, page, inventory ->
            if (page != activePage) return@layoutedForEach
            val inv = inventory.inventory ?: return@layoutedForEach
            val rows = inv.rows
            val gridX = x + 3
            val gridY = y + 5 + font.lineHeight + 1
            if (! inRect(mouseX, mouseY, gridX, gridY, 9 * SLOT_SIZE, rows * SLOT_SIZE)) return@layoutedForEach
            val col = ((mouseX - gridX) / SLOT_SIZE).toInt().coerceIn(0, 8)
            val row = ((mouseY - gridY) / SLOT_SIZE).toInt().coerceIn(0, rows - 1)
            hit = row * 9 + col
        }
        if (hit < 0 || hit >= chestSlots.size) return false
        val player = mc.player ?: return false
        val gameMode = mc.gameMode ?: return false
        val targetSlot = chestSlots[hit]
        val shift = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0
        val clickType = if (shift) ClickType.QUICK_MOVE else ClickType.PICKUP
        gameMode.handleInventoryMouseClick(menu.containerId, targetSlot.index, button, clickType, player)
        return true
    }

    private fun dispatchPlayerInventoryClick(button: Int, modifiers: Int, mouseX: Int, mouseY: Int): Boolean {
        val slotIndex = getPlayerInvIndex(mouseX, mouseY) ?: return false
        val containerScreen = mc.screen as? AbstractContainerScreen<*> ?: return false
        val targetSlot = containerScreen.menu.slots.firstOrNull { it.container is Inventory && it.containerSlot == slotIndex } ?: return false
        val player = mc.player ?: return false
        val gameMode = mc.gameMode ?: return false
        val shift = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0
        val clickType = if (shift) ClickType.QUICK_MOVE else ClickType.PICKUP
        gameMode.handleInventoryMouseClick(containerScreen.menu.containerId, targetSlot.index, button, clickType, player)
        return true
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double, activePage: StoragePageSlot?): Boolean {
        if (activePage != null && StorageOverlay.lockScrollOnActiveSetting.value) {
            val data = StorageOverlay.storageData
            var overActive = false
            layoutedForEach(data) { x, y, pw, ph, page, _ ->
                if (page == activePage && inRect(mouseX, mouseY, x, y, pw, ph)) {
                    overActive = true
                }
            }
            if (overActive) return false
        }
        val speed = verticalAmount * StorageOverlay.scrollSpeedSetting.value * (if (StorageOverlay.inverseScrollSetting.value) 1 else - 1)
        scroll = (scroll + speed.toFloat()).coerceAtMost(maxScroll).coerceAtLeast(0f)
        return true
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int, modifiers: Int, activePage: StoragePageSlot?): Boolean {
        val resolutionMouseX = Resolution.getMouseX(mouseX)
        val resolutionMouseY = Resolution.getMouseY(mouseY)

        if (inRect(resolutionMouseX, resolutionMouseY, scrollPanelX, scrollPanelY, scrollPanelW, scrollPanelH)) {
            val data = StorageOverlay.storageData
            if (activePage != null && dispatchActivePageSlotClick(button, modifiers, resolutionMouseX.toDouble(), resolutionMouseY.toDouble(), activePage)) return true
            layoutedForEach(data) { x, y, pw, ph, page, _ ->
                if (inRect(resolutionMouseX, resolutionMouseY, x, y, pw, ph) && activePage != page && button == 0) {
                    page.navigateTo()
                    return true
                }
            }
            return false
        }

        if (inRect(resolutionMouseX, resolutionMouseY, scrollBarX, scrollBarY, SCROLL_BAR_WIDTH, scrollBarH)) {
            val percentage = ((resolutionMouseY - scrollBarY) / scrollBarH.toDouble()).coerceIn(0.0, 1.0)
            scroll = (maxScroll * percentage).toFloat()
            knobGrabbed = true
            return true
        }

        return dispatchPlayerInventoryClick(button, modifiers, resolutionMouseX, resolutionMouseY)
    }

    fun mouseReleased(): Boolean {
        if (! knobGrabbed) return false
        knobGrabbed = false
        return true
    }

    fun mouseDragged(mouseY: Double): Boolean {
        if (! knobGrabbed) return false
        val percentage = ((Resolution.getMouseY(mouseY) - scrollBarY) / scrollBarH.toDouble()).coerceIn(0.0, 1.0)
        scroll = (maxScroll * percentage).toFloat()
        return true
    }

    fun updateBounds() {
        val screen = containerScreen ?: return
        init(mc, Resolution.width.toInt(), Resolution.height.toInt())
        val accessor = screen as IAbstractContainerScreen
        accessor.setLeftPos(0)
        accessor.setTopPos(0)
        accessor.setImageWidth(screen.width)
        accessor.setImageHeight(screen.height)
    }

    fun renderContainerOverlay(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val screen = containerScreen ?: return
        Resolution.refresh()
        updateBounds()
        Resolution.push(context)
        val scaledMouseX = Resolution.getMouseX(mouseX.toDouble())
        val scaledMouseY = Resolution.getMouseY(mouseY.toDouble())
        Render2D.drawRect(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, Color(24, 24, 27))
        Render2D.drawBorder(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, Color(60, 60, 65))
        val activeSlot = (handler as? StorageBackingHandle.Page)?.storagePageSlot
        val chestSlots = screen.menu.slots.take(screen.menu.rowCount * 9).drop(9)
        context.drawPages(scaledMouseX, scaledMouseY, activeSlot, chestSlots)
        context.drawScrollBar()
        context.drawPlayerInventory(scaledMouseX, scaledMouseY)
        Resolution.pop(context)
    }

    fun renderCarriedItem(context: GuiGraphics, mouseX: Int, mouseY: Int): Boolean {
        val screen = containerScreen ?: return false
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

    fun mouseScrolledFromContainer(mouseX: Double, mouseY: Double, verticalAmount: Double) = mouseScrolled(Resolution.getMouseX(mouseX).toDouble(), Resolution.getMouseY(mouseY).toDouble(), verticalAmount, (handler as? StorageBackingHandle.Page)?.storagePageSlot)
    fun mouseClickFromContainer(click: MouseButtonEvent) = mouseClicked(click.x(), click.y(), click.button(), click.modifiers(), (handler as? StorageBackingHandle.Page)?.storagePageSlot)
    fun isPointOverSlot(slot: Slot, xO: Int, yO: Int, pX: Double, pY: Double) = pX >= slot.x + xO && pX < slot.x + xO + 16 && pY >= slot.y + yO && pY < slot.y + yO + 16
    fun onContainerClose() {
        isExiting = true
        resetScroll()
    }
}