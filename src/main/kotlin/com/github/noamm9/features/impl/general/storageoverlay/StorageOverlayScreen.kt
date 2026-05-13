@file:Suppress("NOTHING_TO_INLINE")

package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.misc.InventorySearch
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.roundToInt

private inline fun inRect(mx: Double, my: Double, x: Int, y: Int, w: Int, h: Int) = mx >= x && mx < x + w && my >= y && my < y + h
private inline fun inRect(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int) = mx >= x && mx < x + w && my >= y && my < y + h

internal class StorageOverlayScreen: Screen(Component.literal("Storage Overlay")) {

    companion object {
        const val SLOT_SIZE = 18
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

    var isExiting = false
    var pageWidthCount = StorageOverlay.columnsSetting.value

    private val panelColor = Color(24, 24, 27, 220)
    private val slotBgColor = Color(50, 50, 55, 200)
    private val slotCellBg = Color(30, 30, 34).rgb
    private val slotCellBorder = Color(55, 55, 60).rgb
    private val activePageBorder = Color(80, 200, 80)
    private val scrollBgColor = Color(30, 30, 35, 180)
    private val scrollKnobColor = Color(120, 120, 130)
    private val borderColor = Color(60, 60, 65)

    inner class Measurements {
        val innerScrollPanelWidth = PAGE_WIDTH * pageWidthCount + (pageWidthCount - 1) * PADDING
        val overviewWidth = innerScrollPanelWidth + 3 * PADDING + SCROLL_BAR_WIDTH
        val x = width / 2 - overviewWidth / 2
        val overviewHeight = minOf(height - PLAYER_HEIGHT - minOf(80, height / 10), StorageOverlay.maxHeightSetting.value)
        val innerScrollPanelHeight = overviewHeight - PADDING * 2
        val y = height / 2 - (overviewHeight + PLAYER_HEIGHT) / 2
        val playerX = width / 2 - PLAYER_WIDTH / 2
        val playerY = y + overviewHeight + 2
        val totalHeight = overviewHeight - 3 + PLAYER_HEIGHT
    }

    var measurements = Measurements()

    public override fun init() {
        super.init()
        Resolution.refresh()
        val oldMax = getMaxScroll()
        val scrollPct = if (oldMax > 0) scroll / oldMax else 0f
        pageWidthCount = StorageOverlay.columnsSetting.value.coerceAtMost((width - PADDING) / (PAGE_WIDTH + PADDING)).coerceAtLeast(1)
        measurements = Measurements()
        val newMax = getMaxScroll()
        scroll = (scrollPct * newMax).coerceAtMost(newMax).coerceAtLeast(0f)
    }

    fun ensureResolutionLayout() {
        Resolution.refresh()
        val resolutionWidth = Resolution.width.roundToInt()
        val resolutionHeight = Resolution.height.roundToInt()
        if (width != resolutionWidth || height != resolutionHeight) {
            init(mc, resolutionWidth, resolutionHeight)
        }
    }

    fun toGuiCoordinate(value: Int): Int = (value * Resolution.scale).roundToInt()

    private fun toResolutionMouseX(mouseX: Double) = Resolution.getMouseX(mouseX)
    private fun toResolutionMouseY(mouseY: Double) = Resolution.getMouseY(mouseY)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return mouseScrolled(mouseX, mouseY, verticalAmount, null)
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
        coerceScroll(adjustScrollSpeed(verticalAmount).toFloat())
        return true
    }

    fun coerceScroll(offset: Float) {
        scroll = (scroll + offset).coerceAtMost(getMaxScroll()).coerceAtLeast(0f)
    }

    fun adjustScrollSpeed(amount: Double) = amount * StorageOverlay.scrollSpeedSetting.value * (if (StorageOverlay.inverseScrollSetting.value) 1 else - 1)

    fun getMaxScroll() = (lastRenderedInnerHeight.toFloat() + 6 - measurements.innerScrollPanelHeight).coerceAtLeast(0f)

    override fun onClose() {
        isExiting = true
        resetScroll()
        super.onClose()
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        ensureResolutionLayout()
        Resolution.push(context)
        drawOverlay(context, toResolutionMouseX(mouseX.toDouble()), toResolutionMouseY(mouseY.toDouble()), delta)
        Resolution.pop(context)
    }

    fun drawOverlay(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        Render2D.drawRect(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, panelColor)
        Render2D.drawBorder(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, borderColor)
        drawPages(context, mouseX, mouseY, delta, null, null)
        drawScrollBar(context)
        drawPlayerInventory(context, mouseX, mouseY)
    }

    private val scrollPanelX get() = measurements.x + PADDING
    private val scrollPanelY get() = measurements.y + PADDING
    private val scrollPanelW get() = measurements.innerScrollPanelWidth
    private val scrollPanelH get() = measurements.innerScrollPanelHeight

    private val scrollBarX get() = measurements.x + PADDING + measurements.innerScrollPanelWidth + PADDING
    private val scrollBarY get() = measurements.y + PADDING
    private val scrollBarH get() = measurements.innerScrollPanelHeight

    fun createScissors(context: GuiGraphics) {
        context.enableScissor(scrollPanelX, scrollPanelY, scrollPanelX + scrollPanelW, scrollPanelY + scrollPanelH)
    }

    fun drawPages(
        context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float,
        excluding: StoragePageSlot?,
        slots: List<Slot>?,
    ) {
        createScissors(context)
        val data = StorageOverlay.storageData
        val viewTop = scrollPanelY
        val viewBottom = scrollPanelY + scrollPanelH
        layoutedForEach(data) { x, y, _, ph, page, inventory ->
            if (y + ph < viewTop || y > viewBottom) return@layoutedForEach
            drawPage(context, x, y, page, inventory, if (excluding == page) slots else null, mouseX, mouseY)
        }
        context.disableScissor()
    }

    fun drawScrollBar(context: GuiGraphics) {
        Render2D.drawRect(context, scrollBarX, scrollBarY, SCROLL_BAR_WIDTH, scrollBarH, scrollBgColor)
        val maxScroll = getMaxScroll()
        val percentage = if (maxScroll > 0) scroll / maxScroll else 0f
        val knobY = scrollBarY + (percentage * (scrollBarH - SCROLL_BAR_HEIGHT)).toInt()
        Render2D.drawRect(context, scrollBarX, knobY, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, scrollKnobColor)
    }

    fun getPlayerInventorySlotPosition(index: Int): Pair<Int, Int> {
        val slotsWidth = 9 * SLOT_SIZE
        val baseX = measurements.playerX + (PLAYER_WIDTH - slotsWidth) / 2 - SLOT_SIZE / 2 + 1
        val baseY = measurements.playerY + 8
        if (index < 9) {
            return Pair(baseX + index * SLOT_SIZE, baseY + 3 * SLOT_SIZE + 4)
        }
        return Pair(baseX + (index % 9) * SLOT_SIZE, baseY + (index / 9 - 1) * SLOT_SIZE)
    }

    private fun drawSlotGrid(context: GuiGraphics, x: Int, y: Int, rows: Int) {
        val w = 9 * SLOT_SIZE
        val h = rows * SLOT_SIZE
        context.fill(x, y, x + w, y + h, slotCellBg)
        for (col in 0 .. 9) {
            val lx = x + col * SLOT_SIZE
            context.fill(lx, y, lx + 1, y + h, slotCellBorder)
        }
        for (row in 0 .. rows) {
            val ly = y + row * SLOT_SIZE
            context.fill(x, ly, x + w, ly + 1, slotCellBorder)
        }
    }

    fun drawPlayerInventory(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val items = mc.player?.inventory?.nonEquipmentItems ?: return
        val (invX, invY) = getPlayerInventorySlotPosition(9)
        val (hotX, hotY) = getPlayerInventorySlotPosition(0)
        var hoveredStack: net.minecraft.world.item.ItemStack? = null

        drawSlotGrid(context, invX - 1, invY - 1, 3)
        drawSlotGrid(context, hotX - 1, hotY - 1, 1)

        for (i in 0 until 36) {
            val item = items[i]
            if (item.isEmpty) continue
            val (sx, sy) = getPlayerInventorySlotPosition(i)
            if (InventorySearch.matches(item)) {
                Render2D.drawRect(context, sx, sy, 16, 16, InventorySearch.color)
            }
            context.renderItem(item, sx, sy, 0)
            context.renderItemDecorations(font, item, sx, sy)

            if (hoveredStack == null && inRect(mouseX, mouseY, sx, sy, 17, 17)) {
                hoveredStack = item
            }
        }

        if (hoveredStack != null) {
            context.setTooltipForNextFrame(font, hoveredStack, toGuiCoordinate(mouseX), toGuiCoordinate(mouseY))
        }
    }

    private fun getPlayerInventoryIndexAt(mouseX: Int, mouseY: Int): Int? {
        for (index in 0 until 36) {
            val (slotX, slotY) = getPlayerInventorySlotPosition(index)
            if (inRect(mouseX, mouseY, slotX, slotY, 17, 17)) return index
        }
        return null
    }

    private inline fun layoutedForEach(
        data: StorageData,
        func: (x: Int, y: Int, pageWidth: Int, pageHeight: Int, page: StoragePageSlot, inventory: StorageData.StorageInventory) -> Unit
    ) {
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

    fun drawPage(
        context: GuiGraphics,
        x: Int, y: Int,
        page: StoragePageSlot,
        inventory: StorageData.StorageInventory,
        slots: List<Slot>?,
        mouseX: Int, mouseY: Int,
    ): Int {
        val inv = inventory.inventory
        if (inv == null && slots == null) {
            Render2D.drawRect(context, x, y, PAGE_WIDTH, 18, slotBgColor)
            Render2D.drawBorder(context, x, y, PAGE_WIDTH, 18, borderColor)
            Render2D.drawString(context, inventory.title + " - Click to load", x + 4f, y + 5f, Color(180, 180, 180))
            return 18
        }
        val rows = inv?.rows ?: (slots?.size?.div(9)?.coerceIn(1, 5) ?: 3)

        val name = inventory.title
        val isActive = slots != null
        val slotsY = y + 5 + font.lineHeight
        val pageHeight = rows * SLOT_SIZE + 8 + font.lineHeight

        if (isActive) {
            Render2D.drawBorder(context, x, y, PAGE_WIDTH + 1, pageHeight, activePageBorder, 2)
        }

        context.drawString(font, Component.literal(name), x + 6, y + 3, if (isActive) activePageBorder.rgb else 0xFFFFFF, true)

        val panelX = scrollPanelX
        val panelY = scrollPanelY
        val panelW = scrollPanelW
        val panelH = scrollPanelH
        var hoveredStack: net.minecraft.world.item.ItemStack? = null
        var hoveredX = 0
        var hoveredY = 0

        drawSlotGrid(context, x + 2, slotsY, rows)

        val invStacks = inv?.stacks
        val itemCount = invStacks?.size ?: (slots?.size ?: (rows * 9))

        for (index in 0 until itemCount) {
            val slotX = (index % 9) * SLOT_SIZE + x + 3
            val slotY = (index / 9) * SLOT_SIZE + slotsY + 1

            if (slotY + 16 < panelY || slotY > panelY + panelH) continue

            val displayStack = if (slots != null && index < slots.size) slots[index].item
            else invStacks?.get(index) ?: continue
            if (displayStack.isEmpty) continue

            if (InventorySearch.matches(displayStack)) {
                Render2D.drawRect(context, slotX, slotY, 16, 16, InventorySearch.color)
            }
            context.renderItem(displayStack, slotX, slotY)
            context.renderItemDecorations(font, displayStack, slotX, slotY)

            if (hoveredStack == null && inRect(mouseX, mouseY, slotX, slotY, 17, 17) && inRect(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
                hoveredStack = displayStack
                hoveredX = mouseX
                hoveredY = mouseY
            }
        }

        if (hoveredStack != null) {
            context.setTooltipForNextFrame(font, hoveredStack, toGuiCoordinate(hoveredX), toGuiCoordinate(hoveredY))
        }
        return pageHeight + 6
    }

    private var knobGrabbed = false

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
        val slotIndex = getPlayerInventoryIndexAt(mouseX, mouseY) ?: return false
        val containerScreen = mc.screen as? AbstractContainerScreen<*> ?: return false
        val targetSlot = containerScreen.menu.slots.firstOrNull {
            it.container is net.minecraft.world.entity.player.Inventory && it.containerSlot == slotIndex
        } ?: return false
        val player = mc.player ?: return false
        val gameMode = mc.gameMode ?: return false
        val shift = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0
        val clickType = if (shift) ClickType.QUICK_MOVE else ClickType.PICKUP
        gameMode.handleInventoryMouseClick(containerScreen.menu.containerId, targetSlot.index, button, clickType, player)
        return true
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean) = mouseClicked(click.x(), click.y(), click.button(), click.modifiers(), null)

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int, modifiers: Int, activePage: StoragePageSlot?): Boolean {
        val resolutionMouseX = toResolutionMouseX(mouseX)
        val resolutionMouseY = toResolutionMouseY(mouseY)

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
            scroll = (getMaxScroll() * percentage).toFloat()
            knobGrabbed = true
            return true
        }

        return dispatchPlayerInventoryClick(button, modifiers, resolutionMouseX, resolutionMouseY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        return mouseReleased()
    }

    fun mouseReleased(): Boolean {
        if (! knobGrabbed) return false
        knobGrabbed = false
        return true
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        return mouseDragged(click.y())
    }

    fun mouseDragged(mouseY: Double): Boolean {
        if (knobGrabbed) {
            val percentage = ((toResolutionMouseY(mouseY) - scrollBarY) / scrollBarH.toDouble()).coerceIn(0.0, 1.0)
            scroll = (getMaxScroll() * percentage).toFloat()
            return true
        }
        return false
    }

    override fun shouldCloseOnEsc(): Boolean = this === mc.screen
}