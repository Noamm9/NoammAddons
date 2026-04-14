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
        context.fill(0, 0, width, height, Color(0, 0, 0, 180).rgb)
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
        layoutedForEach(data) { x, y, _, _, page, inventory ->
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
        for (i in 0 until 36) {
            val (sx, sy) = getPlayerInventorySlotPosition(i)
            Render2D.drawRect(context, sx - 1, sy - 1, SLOT_SIZE, SLOT_SIZE, slotCellColor)
            Render2D.drawBorder(context, sx - 1, sy - 1, SLOT_SIZE, SLOT_SIZE, slotCellBorder)
        }
        items.withIndex().forEach { (index, item) ->
            val (x, y) = getPlayerInventorySlotPosition(index)
            context.renderItem(item, x, y, 0)
            context.renderItemDecorations(font, item, x, y)
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

        val panel = getScrollPanelInner()
        val itemCount = inv?.stacks?.size ?: (slots?.size ?: (rows * 9))
        for (index in 0 until itemCount) {
            val slotX = (index % 9) * SLOT_SIZE + x + 3
            val slotY = (index / 9) * SLOT_SIZE + slotsY + 1

            Render2D.drawRect(context, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE, slotCellColor)
            Render2D.drawBorder(context, slotX - 1, slotY - 1, SLOT_SIZE, SLOT_SIZE, slotCellBorder)

            val displayStack = if (slots != null && index < slots.size) slots[index].item
                else inv?.stacks?.getOrNull(index) ?: net.minecraft.world.item.ItemStack.EMPTY
            context.renderItem(displayStack, slotX, slotY)
            context.renderItemDecorations(font, displayStack, slotX, slotY)

            if (!displayStack.isEmpty && mouseX >= slotX && mouseY >= slotY && mouseX <= slotX + 16 && mouseY <= slotY + 16 && mouseX >= panel[0] && mouseY >= panel[1] && mouseX < panel[0] + panel[2] && mouseY < panel[1] + panel[3]) {
                try {
                    context.setTooltipForNextFrame(font, displayStack, mouseX, mouseY)
                } catch (_: Exception) {}
            }

            if (slots != null && index < slots.size) {
                val screenAccessor = mc.screen as? IAbstractContainerScreen
                val offX = screenAccessor?.leftPos ?: 0
                val offY = screenAccessor?.topPos ?: 0
                slots[index].setSlotX(slotX - offX)
                slots[index].setSlotY(slotY - offY)
            }
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
