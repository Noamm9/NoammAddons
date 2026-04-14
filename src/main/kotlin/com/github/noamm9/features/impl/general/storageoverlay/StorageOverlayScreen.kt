package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.customgui.setSlotX
import com.github.noamm9.ui.customgui.setSlotY
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import java.awt.Color

class StorageOverlayScreen : Screen(Component.literal("Storage Overlay")) {

    companion object {
        const val SLOT_SIZE = 18
        const val PADDING = 10
        const val PAGE_SLOTS_WIDTH = SLOT_SIZE * 9
        const val PAGE_WIDTH = PAGE_SLOTS_WIDTH + 4
        const val SCROLL_BAR_WIDTH = 8
        const val SCROLL_BAR_HEIGHT = 16
        const val PLAYER_SLOTS_WIDTH = SLOT_SIZE * 9
        const val PLAYER_WIDTH = PLAYER_SLOTS_WIDTH + 6
        const val PLAYER_HEIGHT = SLOT_SIZE * 4 + 18
        const val PLAYER_Y_INSET = 3

        var scroll: Float = 0f
        var lastRenderedInnerHeight = 0

        fun resetScroll() {
            if (!StorageOverlay.retainScroll) scroll = 0f
        }
    }

    var isExiting = false
    var pageWidthCount = StorageOverlay.columns

    private val panelColor = Color(24, 24, 27, 220)
    private val slotBgColor = Color(50, 50, 55, 200)
    private val slotCellColor = Color(30, 30, 34)
    private val slotCellBorder = Color(55, 55, 60)
    private val activePageBorder = Color(80, 200, 80)
    private val scrollBgColor = Color(30, 30, 35, 180)
    private val scrollKnobColor = Color(120, 120, 130)
    private val borderColor = Color(60, 60, 65)

    private val cachedSlotCellBg = slotCellColor.rgb
    private val cachedSlotCellBorder = slotCellBorder.rgb

    inner class Measurements {
        val innerScrollPanelWidth = PAGE_WIDTH * pageWidthCount + (pageWidthCount - 1) * PADDING
        val overviewWidth = innerScrollPanelWidth + 3 * PADDING + SCROLL_BAR_WIDTH
        val x = width / 2 - overviewWidth / 2
        val overviewHeight = minOf(height - PLAYER_HEIGHT - minOf(80, height / 10), StorageOverlay.maxHeight)
        val innerScrollPanelHeight = overviewHeight - PADDING * 2
        val y = height / 2 - (overviewHeight + PLAYER_HEIGHT) / 2
        val playerX = width / 2 - PLAYER_WIDTH / 2
        val playerY = y + overviewHeight + 2
        val totalWidth = overviewWidth
        val totalHeight = overviewHeight - PLAYER_Y_INSET + PLAYER_HEIGHT
    }

    var measurements = Measurements()

    public override fun init() {
        super.init()
        pageWidthCount = StorageOverlay.columns.coerceAtMost((width - PADDING) / (PAGE_WIDTH + PADDING)).coerceAtLeast(1)
        measurements = Measurements()
        scroll = scroll.coerceAtMost(getMaxScroll()).coerceAtLeast(0f)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        coerceScroll(StorageOverlay.adjustScrollSpeed(verticalAmount).toFloat())
        return true
    }

    fun coerceScroll(offset: Float) { scroll = (scroll + offset).coerceAtMost(getMaxScroll()).coerceAtLeast(0f) }

    fun getMaxScroll() = (lastRenderedInnerHeight.toFloat() + 6 - measurements.innerScrollPanelHeight).coerceAtLeast(0f)

    override fun onClose() {
        isExiting = true
        resetScroll()
        super.onClose()
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        drawOverlay(context, mouseX, mouseY, delta)
    }

    fun drawOverlay(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        Render2D.drawRect(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, panelColor)
        Render2D.drawBorder(context, measurements.x, measurements.y, measurements.overviewWidth, measurements.overviewHeight, borderColor)
        drawPages(context, mouseX, mouseY, delta, null, null)
        drawScrollBar(context)
        drawPlayerInventory(context, mouseX, mouseY)
    }

    fun getScrollPanelInner(): IntArray {
        return intArrayOf(
            measurements.x + PADDING,
            measurements.y + PADDING,
            measurements.innerScrollPanelWidth,
            measurements.innerScrollPanelHeight
        )
    }

    fun createScissors(context: GuiGraphics) {
        val panel = getScrollPanelInner()
        context.enableScissor(panel[0], panel[1], panel[0] + panel[2], panel[1] + panel[3])
    }

    fun drawPages(
        context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float,
        excluding: StoragePageSlot?,
        slots: List<Slot>?,
    ) {
        createScissors(context)
        val data = StorageOverlay.storageData
        val panel = getScrollPanelInner()
        val viewTop = panel[1]
        val viewBottom = panel[1] + panel[3]
        layoutedForEach(data) { x, y, _, ph, page, inventory ->
            if (y + ph < viewTop || y > viewBottom) return@layoutedForEach
            drawPage(context, x, y, page, inventory, if (excluding == page) slots else null, mouseX, mouseY)
        }
        context.disableScissor()
    }

    fun drawScrollBar(context: GuiGraphics) {
        val sb = getScrollBarRect()
        Render2D.drawRect(context, sb[0], sb[1], sb[2], sb[3], scrollBgColor)
        val maxScroll = getMaxScroll()
        val percentage = if (maxScroll > 0) scroll / maxScroll else 0f
        val knobY = sb[1] + (percentage * (sb[3] - SCROLL_BAR_HEIGHT)).toInt()
        Render2D.drawRect(context, sb[0], knobY, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, scrollKnobColor)
    }

    private fun getScrollBarRect(): IntArray {
        return intArrayOf(
            measurements.x + PADDING + measurements.innerScrollPanelWidth + PADDING,
            measurements.y + PADDING,
            SCROLL_BAR_WIDTH,
            measurements.innerScrollPanelHeight
        )
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

    fun drawPlayerInventory(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val items = mc.player?.inventory?.nonEquipmentItems ?: return
        val (invX, invY) = getPlayerInventorySlotPosition(9)
        val (hotX, hotY) = getPlayerInventorySlotPosition(0)
        val gridW = 9 * SLOT_SIZE
        val invGridX = invX - 1
        val invGridY = invY - 1
        val invGridH = 3 * SLOT_SIZE
        val hotGridX = hotX - 1
        val hotGridY = hotY - 1
        val hotGridH = SLOT_SIZE
        val bg = cachedSlotCellBg
        val bc = cachedSlotCellBorder

        context.fill(invGridX, invGridY, invGridX + gridW, invGridY + invGridH, bg)
        for (col in 0..9) {
            val lx = invGridX + col * SLOT_SIZE
            context.fill(lx, invGridY, lx + 1, invGridY + invGridH, bc)
        }
        for (row in 0..3) {
            val ly = invGridY + row * SLOT_SIZE
            context.fill(invGridX, ly, invGridX + gridW, ly + 1, bc)
        }

        context.fill(hotGridX, hotGridY, hotGridX + gridW, hotGridY + hotGridH, bg)
        for (col in 0..9) {
            val lx = hotGridX + col * SLOT_SIZE
            context.fill(lx, hotGridY, lx + 1, hotGridY + hotGridH, bc)
        }
        for (row in 0..1) {
            val ly = hotGridY + row * SLOT_SIZE
            context.fill(hotGridX, ly, hotGridX + gridW, ly + 1, bc)
        }

        for (i in 0 until 36) {
            val item = items[i]
            if (item.isEmpty) continue
            val (sx, sy) = getPlayerInventorySlotPosition(i)
            context.renderItem(item, sx, sy, 0)
            context.renderItemDecorations(font, item, sx, sy)
        }
    }

    private inline fun layoutedForEach(
        data: StorageData,
        func: (x: Int, y: Int, pageWidth: Int, pageHeight: Int, page: StoragePageSlot, inventory: StorageData.StorageInventory) -> Unit
    ) {
        var yOffset = -scroll.toInt()
        var xOffset = 0
        var maxHeight = 0
        for ((page, inventory) in data.storageInventories.entries) {
            val currentHeight = inventory.inventory?.let { it.rows * SLOT_SIZE + 6 + font.lineHeight } ?: 18
            maxHeight = maxOf(maxHeight, currentHeight)
            val rectX = measurements.x + PADDING + (PAGE_WIDTH + PADDING) * xOffset
            val rectY = yOffset + measurements.y + PADDING
            func(rectX, rectY, PAGE_WIDTH, currentHeight, page, inventory)
            xOffset++
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
            Render2D.drawBorder(context, x, y, PAGE_WIDTH, pageHeight, activePageBorder, 2)
        }

        context.drawString(font, Component.literal(name), x + 6, y + 3, if (isActive) activePageBorder.rgb else 0xFFFFFF, true)

        val panelX = measurements.x + PADDING
        val panelY = measurements.y + PADDING
        val panelW = measurements.innerScrollPanelWidth
        val panelH = measurements.innerScrollPanelHeight
        val screenAccessor = if (isActive) mc.screen as? IAbstractContainerScreen else null
        val offX = screenAccessor?.leftPos ?: 0
        val offY = screenAccessor?.topPos ?: 0
        var hoveredStack: net.minecraft.world.item.ItemStack? = null
        var hoveredX = 0
        var hoveredY = 0

        val gridX = x + 2
        val gridY = slotsY
        val gridW = 9 * SLOT_SIZE
        val gridH = rows * SLOT_SIZE
        context.fill(gridX, gridY, gridX + gridW, gridY + gridH, cachedSlotCellBg)
        for (col in 0..9) {
            val lx = gridX + col * SLOT_SIZE
            context.fill(lx, gridY, lx + 1, gridY + gridH, cachedSlotCellBorder)
        }
        for (row in 0..rows) {
            val ly = gridY + row * SLOT_SIZE
            context.fill(gridX, ly, gridX + gridW, ly + 1, cachedSlotCellBorder)
        }

        val viewBottom = panelY + panelH
        val viewTop = panelY
        val activeSlots = slots
        val invStacks = inv?.stacks
        val itemCount = invStacks?.size ?: (activeSlots?.size ?: (rows * 9))

        for (index in 0 until itemCount) {
            val slotX = (index % 9) * SLOT_SIZE + x + 3
            val slotY = (index / 9) * SLOT_SIZE + slotsY + 1

            if (activeSlots != null && index < activeSlots.size) {
                activeSlots[index].setSlotX(slotX - offX)
                activeSlots[index].setSlotY(slotY - offY)
            }

            if (slotY + 16 < viewTop || slotY > viewBottom) continue

            val displayStack = if (activeSlots != null && index < activeSlots.size) activeSlots[index].item
                else invStacks?.get(index) ?: continue
            if (displayStack.isEmpty) continue

            context.renderItem(displayStack, slotX, slotY)
            context.renderItemDecorations(font, displayStack, slotX, slotY)

            if (hoveredStack == null && mouseX >= slotX && mouseY >= slotY && mouseX <= slotX + 16 && mouseY <= slotY + 16 && mouseX >= panelX && mouseY >= panelY && mouseX < panelX + panelW && mouseY < panelY + panelH) {
                hoveredStack = displayStack
                hoveredX = mouseX
                hoveredY = mouseY
            }
        }

        if (hoveredStack != null) {
            try { context.setTooltipForNextFrame(font, hoveredStack, hoveredX, hoveredY) } catch (_: Exception) {}
        }
        return pageHeight + 6
    }

    private var knobGrabbed = false

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean) = mouseClicked(click, doubled, null)

    fun mouseClicked(click: MouseButtonEvent, doubled: Boolean, activePage: StoragePageSlot?): Boolean {
        val mouseX = click.x()
        val mouseY = click.y()

        val panel = getScrollPanelInner()
        if (mouseX >= panel[0] && mouseX < panel[0] + panel[2] && mouseY >= panel[1] && mouseY < panel[1] + panel[3]) {
            val data = StorageOverlay.storageData
            layoutedForEach(data) { x, y, pw, ph, page, _ ->
                if (mouseX >= x && mouseX < x + pw && mouseY >= y && mouseY < y + ph && activePage != page && click.button() == 0) {
                    page.navigateTo()
                    return true
                }
            }
            return false
        }

        val sb = getScrollBarRect()
        if (mouseX >= sb[0] && mouseX < sb[0] + sb[2] && mouseY >= sb[1] && mouseY < sb[1] + sb[3]) {
            val percentage = ((mouseY - sb[1]) / sb[3].toDouble()).coerceIn(0.0, 1.0)
            scroll = (getMaxScroll() * percentage).toFloat()
            knobGrabbed = true
            return true
        }

        return false
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        if (knobGrabbed) { knobGrabbed = false; return true }
        return super.mouseReleased(click)
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        if (knobGrabbed) {
            val sb = getScrollBarRect()
            val percentage = ((click.y() - sb[1]) / sb[3].toDouble()).coerceIn(0.0, 1.0)
            scroll = (getMaxScroll() * percentage).toFloat()
            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun shouldCloseOnEsc(): Boolean = this === mc.screen
}
