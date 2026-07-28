package com.github.noamm9.ui.utils

import com.github.noamm9.utils.render.Render2D.drawAnnularSegment
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawLine
import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class WheelMenu<T>(
    private val pageSize: Int,
    private val scale: () -> Float = { 1f },
    private val style: () -> Style,
    private val isActive: (T) -> Boolean = { false },
    private val title: (page: Int, pageCount: Int) -> String,
    private val renderEntry: (GuiGraphicsExtractor, EntryRender<T>) -> Unit,
    private val renderCenter: (GuiGraphicsExtractor, CenterRender<T>) -> Unit = { _, _ -> },
    private val finishContentRender: (GuiGraphicsExtractor) -> Unit = {}
) {
    init {
        require(pageSize > 0) { "pageSize must be greater than zero" }
    }

    var page = 0
        private set

    fun reset() {
        page = 0
    }

    fun onRender(context: GuiGraphicsExtractor, entries: List<T>, mouseX: Int, mouseY: Int) {
        Resolution.push(context)
        try {
            clampPage(entries.size)

            val visibleEntries = entriesOnCurrentPage(entries)
            val layout = layout(visibleEntries.size)
            val hoveredIndex = hoveredIndex(mouseX.toDouble(), mouseY.toDouble(), layout)
            val selectedEntry = hoveredIndex?.let(visibleEntries::getOrNull) ?: visibleEntries.firstOrNull(isActive)
            val currentStyle = style()

            currentStyle.backgroundColor?.let {
                context.drawRect(0, 0, Resolution.width, Resolution.height, it)
            }

            visibleEntries.forEachIndexed { index, entry ->
                drawSegment(context, layout, index, currentStyle.segmentColor)
                if (isActive(entry)) drawSegment(context, layout, index, currentStyle.activeColor)
                if (index == hoveredIndex) drawSegment(context, layout, index, currentStyle.hoverColor)
            }

            drawSeparators(context, layout, currentStyle)

            visibleEntries.forEachIndexed { index, entry ->
                renderEntry(context, EntryRender(entry, index, index == hoveredIndex, layout))
            }

            selectedEntry?.let {
                renderCenter(context, CenterRender(it, isActive(it), layout))
            }
            finishContentRender(context)

            context.drawCenteredString(
                title(page + 1, pageCount(entries.size)),
                layout.centerX,
                layout.centerY - layout.outerRadius - 19f,
                scale = 0.85f
            )
        }
        finally {
            Resolution.pop(context)
        }
    }

    fun onClick(entries: List<T>, mouseX: Double, mouseY: Double, button: Int, modifiers: Int): EntryClick<T>? {
        clampPage(entries.size)
        val visibleEntries = entriesOnCurrentPage(entries)
        val index = hoveredIndex(mouseX, mouseY, layout(visibleEntries.size)) ?: return null
        val entry = visibleEntries.getOrNull(index) ?: return null
        return EntryClick(entry, index, page * pageSize + index, button, modifiers)
    }

    fun onScroll(entryCount: Int, amount: Double): Boolean {
        if (amount == 0.0) return false

        clampPage(entryCount)
        val pageCount = pageCount(entryCount)
        if (pageCount <= 1) return false

        page = Math.floorMod(page + if (amount < 0.0) 1 else - 1, pageCount)
        return true
    }

    fun entryAt(entries: List<T>, pageIndex: Int): T? {
        clampPage(entries.size)
        if (pageIndex !in 0 until pageSize) return null
        return entries.getOrNull(page * pageSize + pageIndex)
    }

    fun pageCount(entryCount: Int) = Math.ceilDiv(entryCount, pageSize).coerceAtLeast(1)

    private fun clampPage(entryCount: Int) {
        page = page.coerceIn(0, pageCount(entryCount) - 1)
    }

    private fun entriesOnCurrentPage(entries: List<T>) = entries.drop(page * pageSize).take(pageSize)

    private fun layout(segmentCount: Int): Layout {
        val desiredRadius = 138f * scale()
        val maxRadius = min((Resolution.height - 96f) / 2f, (Resolution.width - 220f) / 2f).coerceAtLeast(82f)
        val outerRadius = min(desiredRadius, maxRadius)
        return Layout(
            centerX = Resolution.width / 2f,
            centerY = Resolution.height / 2f - 8f,
            innerRadius = outerRadius * 0.55f,
            outerRadius = outerRadius,
            segmentCount = segmentCount
        )
    }

    private fun hoveredIndex(mouseX: Double, mouseY: Double, layout: Layout): Int? {
        if (layout.segmentCount == 0) return null

        val x = Resolution.getMouseX(mouseX) - layout.centerX
        val y = Resolution.getMouseY(mouseY) - layout.centerY
        if (hypot(x.toDouble(), y.toDouble()) <= layout.innerRadius) return null

        return Math.floorMod(
            floor((atan2(y, x) + PI / 2.0 + layout.segmentAngle / 2.0) / layout.segmentAngle).toInt(),
            layout.segmentCount
        )
    }

    private fun drawSegment(context: GuiGraphicsExtractor, layout: Layout, index: Int, color: Color) {
        val centerAngle = layout.angleFor(index)
        context.drawAnnularSegment(
            layout.centerX,
            layout.centerY,
            layout.innerRadius,
            layout.outerRadius,
            centerAngle - layout.segmentAngle / 2.0,
            centerAngle + layout.segmentAngle / 2.0,
            color
        )
    }

    private fun drawSeparators(context: GuiGraphicsExtractor, layout: Layout, style: Style) {
        if (layout.segmentCount <= 1) return

        repeat(layout.segmentCount) { index ->
            val angle = - PI / 2.0 - layout.segmentAngle / 2.0 + index * layout.segmentAngle
            val cosAngle = cos(angle).toFloat()
            val sinAngle = sin(angle).toFloat()
            val innerX = layout.centerX + cosAngle * (layout.innerRadius - 1f)
            val innerY = layout.centerY + sinAngle * (layout.innerRadius - 1f)
            val outerX = layout.centerX + cosAngle * (layout.outerRadius + 1f)
            val outerY = layout.centerY + sinAngle * (layout.outerRadius + 1f)
            context.drawLine(innerX, innerY, outerX, outerY, style.separatorColor, style.separatorWidth)
        }
    }

    data class Style(
        val segmentColor: Color,
        val hoverColor: Color,
        val activeColor: Color,
        val separatorColor: Color,
        val backgroundColor: Color? = null,
        val separatorWidth: Float = 2f
    )

    data class Layout(
        val centerX: Float,
        val centerY: Float,
        val innerRadius: Float,
        val outerRadius: Float,
        val segmentCount: Int
    ) {
        val segmentAngle = if (segmentCount == 0) 0.0 else PI * 2.0 / segmentCount

        fun angleFor(index: Int) = - PI / 2.0 + index * segmentAngle
    }

    data class EntryRender<T>(
        val entry: T,
        val index: Int,
        val hovered: Boolean,
        val layout: Layout
    ) {
        val angle get() = layout.angleFor(index)
    }

    data class CenterRender<T>(
        val entry: T,
        val active: Boolean,
        val layout: Layout
    )

    data class EntryClick<T>(
        val entry: T,
        val pageIndex: Int,
        val absoluteIndex: Int,
        val button: Int,
        val modifiers: Int
    )
}
