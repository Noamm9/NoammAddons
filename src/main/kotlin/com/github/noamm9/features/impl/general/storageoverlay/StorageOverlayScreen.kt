@file:Suppress("NOTHING_TO_INLINE")

package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.features.impl.general.FEAT_ItemRarity
import com.github.noamm9.features.impl.misc.InventorySearch
import com.github.noamm9.features.impl.misc.ScrollableTooltip
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.*

private inline fun inRect(mx: Double, my: Double, x: Int, y: Int, w: Int, h: Int) = mx >= x && mx < x + w && my >= y && my < y + h
private inline fun inRect(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int) = mx >= x && mx < x + w && my >= y && my < y + h

class StorageOverlayScreen: Screen(Component.literal("Storage Overlay")) {
    private companion object {
        const val SLOT_SIZE = 17 /// 17x17 instead of 16x16 because the border thickness is 1
        const val PADDING = 10
        const val PAGE_WIDTH = SLOT_SIZE * 9 + 4
        const val ACTIVE_PAGE_BORDER_THICKNESS = 2
        const val SCROLL_BAR_WIDTH = 8
        const val SCROLL_BAR_HEIGHT = 16
        const val PLAYER_WIDTH = SLOT_SIZE * 9 + 6
        const val PLAYER_HEIGHT = SLOT_SIZE * 4 + 18

        var lastRenderedInnerHeight = 0
        var scroll: Float = 0f
    }

    private val menuBackgroundColor = Color(24, 24, 27)
    private val menuBorderColor = Color(60, 60, 65)
    private val slotBgColor = Color(50, 50, 55, 200)
    private val slotCellBg = Color(30, 30, 34).rgb
    private val slotCellBorder = Color(55, 55, 60).rgb
    private val activePageBorder get() = ClickGui.accsentColor.value
    private val scrollBgColor = Color(30, 30, 35, 180)
    private val scrollKnobColor = Color(120, 120, 130)

    var isExiting = false
    private var pageWidthCount = StorageOverlay.columnsSetting.value
    private var knobGrabbed = false
    private var hoveredOverlayItem: ItemStack? = null

    var containerScreen: ContainerScreen? = null
    var pendingCenterPage: StoragePage? = null
    var storageMenu: StorageMenu? = null

    private inner class Measurements {
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

    private fun centerOnPage(target: StoragePage) {
        val rows = StorageOverlay.storageMenuData.entries.chunked(pageWidthCount)
        var y = 0
        var center = - 1f
        for (row in rows) {
            val rowH = row.maxOf { (_, inv) -> inv?.let { it.rows * SLOT_SIZE + 6 + font.lineHeight } ?: 18 }
            if (row.any { (page, _) -> page == target }) center = y + rowH / 2f - scrollPanelH / 2f
            y += rowH
        }
        if (center < 0) return
        scroll = center.coerceIn(0f, (y + 6f - scrollPanelH).coerceAtLeast(0f))
        pendingCenterPage = null
    }

    private fun resetTooltip(prev: ItemStack?) {
        if (hoveredOverlayItem === prev) return
        ScrollableTooltip.scrollAmountX = 0f
        ScrollableTooltip.scrollAmountY = 0f
        ScrollableTooltip.scaleOverride = 0f
    }

    private fun GuiGraphicsExtractor.drawPages(mouseX: Int, mouseY: Int, excluding: StoragePage?, slots: List<Slot>?, originalMouseX: Int, originalMouseY: Int) {
        enableScissor(scrollPanelX, scrollPanelY, scrollPanelX + scrollPanelW + ACTIVE_PAGE_BORDER_THICKNESS, scrollPanelY + scrollPanelH)
        val data = StorageOverlay.storageMenuData
        val viewTop = scrollPanelY
        val viewBottom = scrollPanelY + scrollPanelH
        layoutedForEach(data) { x, y, _, ph, page, inventory ->
            if (y + ph < viewTop || y > viewBottom) return@layoutedForEach
            drawPage(x, y, page, inventory, if (excluding == page) slots else null, mouseX, mouseY, originalMouseX, originalMouseY)
        }

        ItemRenderer.endItemRendererBatch(this)
        disableScissor()
    }

    private fun GuiGraphicsExtractor.drawPagesDecorations(excluding: StoragePage?, slots: List<Slot>?) {
        enableScissor(scrollPanelX, scrollPanelY, scrollPanelX + scrollPanelW + ACTIVE_PAGE_BORDER_THICKNESS, scrollPanelY + scrollPanelH)
        val data = StorageOverlay.storageMenuData
        val viewTop = scrollPanelY
        val viewBottom = scrollPanelY + scrollPanelH
        layoutedForEach(data) { x, y, _, ph, page, inventory ->
            if (y + ph < viewTop || y > viewBottom) return@layoutedForEach
            val rows = inventory?.rows ?: (if (excluding == page) slots?.size?.div(9)?.coerceIn(1, 5) ?: 3 else 0)
            if (rows == 0 && inventory == null) return@layoutedForEach

            val slotsY = y + 5 + font.lineHeight
            val invStacks = inventory?.stacks
            val itemCount = invStacks?.size ?: (if (excluding == page) slots?.size ?: (rows * 9) else 0)

            for (index in 0 until itemCount) {
                val slotX = (index % 9) * SLOT_SIZE + x + 3
                val slotY = (index / 9) * SLOT_SIZE + slotsY + 1
                if (slotY + 16 < viewTop || slotY > viewBottom) continue
                val displayStack = if (excluding == page && slots != null && index < slots.size) slots[index].item else invStacks?.get(index) ?: continue
                if (! displayStack.isEmpty) itemDecorations(mc.font, displayStack, slotX, slotY)
            }
        }
        disableScissor()
    }

    private fun GuiGraphicsExtractor.drawScrollBar() {
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

    private fun GuiGraphicsExtractor.drawSlotGrid(x: Int, y: Int, rows: Int) {
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

    private fun GuiGraphicsExtractor.drawPlayerInventory(mouseX: Int, mouseY: Int, originalMouseX: Int, originalMouseY: Int) {
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
                if (FEAT_ItemRarity.enabled) FEAT_ItemRarity.onSlotDraw(this, item, sx, sy)
                if (InventorySearch.matches(item)) {
                    Render2D.drawRect(this, sx, sy, 16, 16, InventorySearch.color)
                }

                ItemRenderer.drawBatchedItemStack(this, item, sx, sy)

                if (hoveredStack == null && isSlotHovered) hoveredStack = item
            }

            if (isSlotHovered) Render2D.drawRect(this, sx, sy, 16, 16, Color.white.withAlpha(50))
        }

        ItemRenderer.endItemRendererBatch(this)

        if (hoveredStack != null) {
            hoveredOverlayItem = hoveredStack
            setTooltipForNextFrame(font, hoveredStack, originalMouseX, originalMouseY)
        }
    }

    private fun GuiGraphicsExtractor.drawPlayerInventoryDecorations() {
        val items = mc.player?.inventory?.nonEquipmentItems ?: return
        for (i in 0 until 36) {
            val item = items[i]
            val (sx, sy) = getPlayerInvSlotPos(i)
            if (! item.isEmpty) itemDecorations(mc.font, item, sx, sy)
        }
    }

    private fun GuiGraphicsExtractor.drawPage(x: Int, y: Int, page: StoragePage, inventory: NBTInventory?, slots: List<Slot>?, mouseX: Int, mouseY: Int, originalMouseX: Int, originalMouseY: Int): Int {
        if (inventory == null && slots == null) {
            Render2D.drawRect(this, x, y, PAGE_WIDTH, 18, slotBgColor)
            Render2D.drawBorder(this, x, y, PAGE_WIDTH, 18, menuBorderColor)
            Render2D.drawString(this, page.name + " - Click to load", x + 4f, y + 5f, Color(180, 180, 180))
            return 18
        }
        val rows = inventory?.rows ?: (slots?.size?.div(9)?.coerceIn(1, 5) ?: 3)

        val name = page.name
        val isActive = slots != null
        val slotsY = y + 5 + font.lineHeight
        val pageHeight = rows * SLOT_SIZE + 8 + font.lineHeight

        if (isActive) {
            Render2D.drawBorder(this, x, y, PAGE_WIDTH + 1, pageHeight, activePageBorder, ACTIVE_PAGE_BORDER_THICKNESS)
        }

        text(font, Component.literal(name), x + 6, y + 3, if (isActive) activePageBorder.rgb else 0xFFFFFF, true)

        val panelX = scrollPanelX
        val panelY = scrollPanelY
        val panelW = scrollPanelW
        val panelH = scrollPanelH
        var hoveredStack: ItemStack? = null

        drawSlotGrid(x + 2, slotsY, rows)

        val invStacks = inventory?.stacks
        val itemCount = invStacks?.size ?: (slots?.size ?: (rows * 9))

        for (index in 0 until itemCount) {
            val slotX = (index % 9) * SLOT_SIZE + x + 3
            val slotY = (index / 9) * SLOT_SIZE + slotsY + 1

            if (slotY + 16 < panelY || slotY > panelY + panelH) continue
            val displayStack = if (slots != null && index < slots.size) slots[index].item else invStacks?.get(index) ?: continue
            val isSlotHovered = inRect(mouseX, mouseY, slotX, slotY, 16, 16) && inRect(mouseX, mouseY, panelX, panelY, panelW, panelH)

            if (! displayStack.isEmpty) {
                if (FEAT_ItemRarity.enabled) FEAT_ItemRarity.onSlotDraw(this, displayStack, slotX, slotY)
                if (InventorySearch.matches(displayStack)) {
                    Render2D.drawRect(this, slotX, slotY, 16, 16, InventorySearch.color)
                }

                ItemRenderer.drawBatchedItemStack(this, displayStack, slotX, slotY)

                if (isSlotHovered && hoveredStack == null) {
                    hoveredStack = displayStack
                }
            }

            if (isSlotHovered) Render2D.drawRect(this, slotX, slotY, 16, 16, Color.white.withAlpha(50))
        }

        if (hoveredStack != null) {
            if (isActive) hoveredOverlayItem = hoveredStack
            setTooltipForNextFrame(font, hoveredStack, originalMouseX, originalMouseY)
        }

        return pageHeight + 6
    }

    private inline fun layoutedForEach(data: SortedMap<StoragePage, NBTInventory?>, func: (x: Int, y: Int, pageWidth: Int, pageHeight: Int, page: StoragePage, inventory: NBTInventory?) -> Unit) {
        var yOffset = - scroll.toInt()
        var xOffset = 0
        var maxHeight = 0
        for ((page, inventory) in data.entries) {
            val currentHeight = inventory?.let { it.rows * SLOT_SIZE + 6 + font.lineHeight } ?: 18
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

    private fun dispatchActivePageSlotClick(button: Int, modifiers: Int, mouseX: Double, mouseY: Double, activePage: StoragePage): Boolean {
        val containerScreen = mc.screen as? AbstractContainerScreen<*> ?: return false
        val menu = containerScreen.menu
        val chestSlots = menu.slots.take(menu.slots.size - 36).drop(9)
        if (chestSlots.isEmpty()) return false

        var hit = - 1
        val data = StorageOverlay.storageMenuData
        layoutedForEach(data) { x, y, _, _, page, inventory ->
            if (page != activePage) return@layoutedForEach
            val inv = inventory ?: return@layoutedForEach
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
        val clickType = if (shift) ContainerInput.QUICK_MOVE else ContainerInput.PICKUP
        gameMode.handleContainerInput(menu.containerId, targetSlot.index, button, clickType, player)
        return true
    }

    private fun dispatchPlayerInventoryClick(button: Int, modifiers: Int, mouseX: Int, mouseY: Int): Boolean {
        val slotIndex = getPlayerInvIndex(mouseX, mouseY) ?: return false
        val containerScreen = mc.screen as? AbstractContainerScreen<*> ?: return false
        val targetSlot = containerScreen.menu.slots.firstOrNull { it.container is Inventory && it.containerSlot == slotIndex } ?: return false
        val player = mc.player ?: return false
        val gameMode = mc.gameMode ?: return false
        val shift = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0
        val clickType = if (shift) ContainerInput.QUICK_MOVE else ContainerInput.PICKUP
        gameMode.handleContainerInput(containerScreen.menu.containerId, targetSlot.index, button, clickType, player)
        return true
    }

    @Suppress("SameReturnValue")
    fun mouseScrolled(verticalAmount: Double): Boolean {
        if (hoveredOverlayItem != null && ScrollableTooltip.enabled && StorageOverlay.enableTooltipInStorage.value) {
            val scroll = (verticalAmount * ScrollableTooltip.scrollSpeed.value).toFloat()
            val holdingShift = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            val holdingCtrl = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            when {
                holdingShift && ! holdingCtrl -> ScrollableTooltip.scrollAmountX -= scroll
                ! holdingShift && holdingCtrl -> ScrollableTooltip.scaleOverride += (verticalAmount / 10f).toFloat() * ScrollableTooltip.scaleSpeed.value.toFloat()
                else -> ScrollableTooltip.scrollAmountY += scroll
            }
            return true
        }

        val speed = verticalAmount * StorageOverlay.scrollSpeedSetting.value * - 1
        scroll = (scroll + speed.toFloat()).coerceAtMost(maxScroll).coerceAtLeast(0f)
        return true
    }

    fun mouseClicked(click: MouseButtonEvent): Boolean {
        val activePage = (storageMenu as? StorageMenu.Page)?.storagePage
        val button = click.button()
        val modifiers = click.modifiers()

        val scale = StorageOverlay.scaleSetting.value
        val resolutionMouseX = Resolution.getMouseX(click.x()) / scale.toDouble()
        val resolutionMouseY = Resolution.getMouseY(click.y()) / scale.toDouble()

        if (inRect(resolutionMouseX, resolutionMouseY, scrollPanelX, scrollPanelY, scrollPanelW, scrollPanelH)) {
            val data = StorageOverlay.storageMenuData
            if (activePage != null && dispatchActivePageSlotClick(button, modifiers, resolutionMouseX, resolutionMouseY, activePage)) return true
            layoutedForEach(data) { x, y, pw, ph, page, _ ->
                if (inRect(resolutionMouseX, resolutionMouseY, x, y, pw, ph) && activePage != page && button == 0) {
                    page.open()
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

        return dispatchPlayerInventoryClick(button, modifiers, resolutionMouseX.toInt(), resolutionMouseY.toInt())
    }

    fun mouseReleased(): Boolean {
        if (! knobGrabbed) return false
        knobGrabbed = false
        return true
    }

    fun mouseDragged(mouseY: Double): Boolean {
        if (! knobGrabbed) return false
        val scale = StorageOverlay.scaleSetting.value
        val percentage = ((Resolution.getMouseY(mouseY) / scale - scrollBarY) / scrollBarH.toDouble()).coerceIn(0.0, 1.0)
        scroll = (maxScroll * percentage).toFloat()
        return true
    }

    fun updateBounds() {
        val screen = containerScreen ?: return
        val scale = StorageOverlay.scaleSetting.value
        init((Resolution.width / scale).toInt(), (Resolution.height / scale).toInt())
        val accessor = screen as IAbstractContainerScreen
        accessor.setLeftPos(0)
        accessor.setTopPos(0)
        accessor.setImageWidth(screen.width)
        accessor.setImageHeight(screen.height)
    }

    fun renderContainerOverlay(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val screen = containerScreen ?: return
        Resolution.refresh()
        updateBounds()
        pendingCenterPage?.let(::centerOnPage)
        val prevHovered = hoveredOverlayItem
        hoveredOverlayItem = null
        Resolution.push(context)
        val scale = StorageOverlay.scaleSetting.value
        context.pose().scale(scale)
        val scaledMouseX = (Resolution.getMouseX(mouseX.toDouble()) / scale).toInt()
        val scaledMouseY = (Resolution.getMouseY(mouseY.toDouble()) / scale).toInt()
        Render2D.drawRect(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, menuBackgroundColor)
        Render2D.drawBorder(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, Color(60, 60, 65))
        val activeSlot = (storageMenu as? StorageMenu.Page)?.storagePage
        val chestSlots = screen.menu.slots.take(screen.menu.rowCount * 9).drop(9)

        context.drawPages(scaledMouseX, scaledMouseY, activeSlot, chestSlots, mouseX, mouseY)
        context.drawScrollBar()
        context.drawPlayerInventory(scaledMouseX, scaledMouseY, mouseX, mouseY)

        context.drawPagesDecorations(activeSlot, chestSlots)
        context.drawPlayerInventoryDecorations()

        Resolution.pop(context)
        resetTooltip(prevHovered)
    }

    fun renderCarriedItem(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int): Boolean {
        val screen = containerScreen ?: return false
        val carried = screen.menu.carried
        if (carried.isEmpty) return true

        Resolution.refresh()
        Resolution.push(context)
        val scale = StorageOverlay.scaleSetting.value
        context.pose().scale(scale)
        val scaledMouseX = (Resolution.getMouseX(mouseX.toDouble()) / scale).toInt() - 8
        val scaledMouseY = (Resolution.getMouseY(mouseY.toDouble()) / scale).toInt() - 8
        ItemRenderer.drawBatchedItemStack(context, carried, scaledMouseX, scaledMouseY)
        ItemRenderer.endItemRendererBatch(context)
        context.itemDecorations(screen.font, carried, scaledMouseX, scaledMouseY)
        Resolution.pop(context)
        return true
    }

    fun isPointOverSlot(slot: Slot, xO: Int, yO: Int, pX: Double, pY: Double) = inRect(pX, pY, slot.x + xO, slot.y + yO, 16, 16)
    fun onContainerClose() {
        if (! StorageOverlay.retainScrollSetting.value) scroll = 0f
        isExiting = true
    }
}