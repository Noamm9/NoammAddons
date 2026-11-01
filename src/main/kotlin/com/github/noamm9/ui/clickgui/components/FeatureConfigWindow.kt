package com.github.noamm9.ui.clickgui.components

import com.mojang.blaze3d.platform.InputConstants

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.TooltipManager
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.clickgui.components.settings.WidgetFactory
import com.github.noamm9.ui.clickgui.components.settings.impl.CategoryWidget
import com.github.noamm9.ui.clickgui.components.settings.impl.SeparatorWidget
import com.github.noamm9.ui.clickgui.enums.WindowClickAction
import com.github.noamm9.ui.utils.MouseHelper
import com.github.noamm9.ui.utils.ResizeCorner
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.ScrollState
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

open class FeatureConfigWindow(val feature: Feature) {
    companion object {
        private const val titleBarHeight = 24f
        private const val windowPadding = 10f
        private const val bottomPadding = 8f
        private const val resizeMargin = 3f
        private const val closeButtonSize = 14f
        private const val settingSpacing = 5f
        private const val categoryWidth = 100f
        private const val categoryHeight = 20f
    }

    var x = 0f
        private set
    var y = 0f
        private set
    protected var width = 0f
        private set
    protected var height = 0f
        private set

    open val defaultWidth get() = 250f + sidebarWidth + previewWidth
    open val defaultHeight = 260f
    protected open val minWidth get() = 180f + sidebarWidth + previewWidth
    protected open val minHeight = 140f

    protected open val previewWidth = 0f

    protected open val useSidebar = false

    private var visibleWidgets = emptyList<Widget<*>>()
    private val scroll = ScrollState(minThumbHeight = 18f)

    private val widgets = feature.configSettings.mapNotNull(WidgetFactory::fromSetting)

    val settings = buildList {
        var lastSection: String? = null

        for (widget in widgets) {
            val section = widget.config.section
            if (section != null && section != lastSection) {
                val sectionSettings = widgets.dropWhile { it !== widget }.takeWhile { it === widget || it.config.section == null }
                val sectionVisibility = { sectionSettings.any { it.config.visibility() } }
                if (isNotEmpty()) add(SeparatorWidget().also { it.config.visibility = sectionVisibility })
                add(CategoryWidget(section).also { it.config.visibility = sectionVisibility })
                lastSection = section
            }
            add(widget)
        }
    }

    private val categories = buildList<Category> {
        var current: Category? = null
        for (widget in widgets) {
            val section = widget.config.section
            if (current == null || (section != null && section != current.name)) {
                current = Category(section ?: "General").also(::add)
            }
            current.widgets.add(widget)
        }
    }

    private var selectedCategory = categories.firstOrNull()

    private var contentLeft = 0f
    private var contentTop = 0f
    private var contentRight = 0f
    private var contentBottom = 0f

    protected var previewX = 0f
        private set
    protected var previewY = 0f
        private set
    protected var previewHeight = 0f
        private set

    var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    private var resizeCorner = ResizeCorner.NONE
    private var interactionStartMouseX = 0f
    private var interactionStartMouseY = 0f
    private var interactionStartX = 0f
    private var interactionStartY = 0f
    private var interactionStartWidth = 0f
    private var interactionStartHeight = 0f

    private var highlightedWidget: Widget<*>? = null
    private var highlightStart = 0L

    fun place(startX: Float, startY: Float) {
        x = startX
        y = startY
        width = defaultWidth
        height = defaultHeight
        clampToScreen()
    }

    protected open fun drawPreview(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float) {}

    protected open fun drawBackground(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, color: Color) {
        context.drawRect(x, y, width, height, color)
    }

    fun render(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, focused: Boolean) {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()

        updateInteraction(mx, my)
        clampToScreen()
        updateLayout()

        val frameColor = if (focused) Color(255, 255, 255, 40) else Color(255, 255, 255, 24)
        val closeHovered = isInsideCloseButton(mx, my)

        drawBackground(context, x - 2f, y - 2f, width + 4f, height + 4f, Color(0, 0, 0, 70))
        drawBackground(context, x, y, width, height, Color(16, 16, 16, 235))
        if (useSidebar) context.drawRect(x, y + titleBarHeight, categoryWidth, height - titleBarHeight, Color(10, 10, 10, 160))
        if (previewWidth > 0f) drawBackground(context, previewX, previewY, previewWidth, previewHeight, Color(10, 10, 10, 160))
        context.drawRect(x, y, width, titleBarHeight, Color(24, 24, 24, 245))
        context.drawRect(x, y, width, 2f, Style.accentColor.withAlpha(if (focused) 255 else 180))
        context.drawBorder(x, y, width, height, frameColor)

        context.drawCenteredString("§l${feature.name}", x + (width / 2f), y + 8f)
        drawCloseButton(context, closeHovered)

        if (useSidebar) drawCategories(context, mx, my)
        drawSettings(context, mouseX, mouseY)

        if (previewWidth > 0f) {
            context.enableScissor(previewX.toInt(), previewY.toInt(), (previewX + previewWidth).toInt(), (previewY + previewHeight).toInt())
            drawPreview(context, previewX, previewY, previewWidth, previewHeight)
            context.disableScissor()
        }
    }

    private fun drawCategories(context: GuiGraphicsExtractor, mx: Float, my: Float) {
        shownCategories.forEachIndexed { index, category ->
            val catY = categoryY(index)
            val isHovered = mx >= x && mx <= x + categoryWidth && my >= catY && my <= catY + categoryHeight
            val isSelected = selectedCategory == category

            if (isSelected) context.drawRect(x, catY, categoryWidth, categoryHeight, Style.accentColor.withAlpha(40))
            if (isHovered && ! isSelected) context.drawRect(x, catY, categoryWidth, categoryHeight, Color(255, 255, 255, 10))

            val color = if (isSelected) Style.accentColor else Color.GRAY
            context.drawString(category.name, x + 10, catY + 6, color)
        }
    }

    private fun drawSettings(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (useSidebar && selectedCategory !in shownCategories) selectedCategory = shownCategories.firstOrNull() ?: categories.firstOrNull()
        visibleWidgets = currentWidgets.filter { it.config.visibility.invoke() }

        val viewportWidth = (contentRight - contentLeft).coerceAtLeast(120f)
        val viewportHeight = (contentBottom - contentTop).coerceAtLeast(40f)
        val totalContentHeight = if (visibleWidgets.isEmpty()) 0f
        else visibleWidgets.sumOf { it.height }.toFloat() + ((visibleWidgets.size - 1) * settingSpacing)

        scroll.update(totalContentHeight, viewportHeight)

        val scrollbarReserve = if (scroll.canScroll) 6f else 0f
        val settingWidth = (viewportWidth - scrollbarReserve).toInt().coerceAtLeast(120)

        context.enableScissor(contentLeft.toInt(), contentTop.toInt(), contentRight.toInt(), contentBottom.toInt())

        var currentY = contentTop - scroll.value

        visibleWidgets.forEach { setting ->
            setting.x = contentLeft.toInt()
            setting.y = currentY.toInt()
            setting.width = settingWidth

            setting.draw(context, mouseX, mouseY)

            val isHovered = mouseX >= setting.x && mouseX <= setting.x + setting.width &&
                mouseY >= setting.y && mouseY <= setting.y + setting.height &&
                isInsideContent(mouseX.toFloat(), mouseY.toFloat())

            if (isHovered) {
                TooltipManager.hover(setting.config.description, mouseX, mouseY)
            }

            currentY += setting.height + settingSpacing
        }

        highlightedWidget?.let { widget ->
            val age = System.currentTimeMillis() - highlightStart
            if (age > 1200) highlightedWidget = null
            else if (age > 0) {
                val alpha = (255 * (1f - age / 1200f)).toInt()
                context.drawBorder(widget.x, widget.y, widget.width, widget.height, Style.accentColor.withAlpha(alpha))
            }
        }

        context.disableScissor()

        scroll.drawScrollbar(context, contentRight - 2f, contentTop, 2f, viewportHeight, mouseX.toFloat(), mouseY.toFloat())
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): WindowClickAction {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()

        if (button == InputConstants.MOUSE_BUTTON_LEFT && isInsideCloseButton(mx, my)) {
            return WindowClickAction.CLOSE
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            val hoveredHandle = getResizeCorner(mx, my)
            if (hoveredHandle != ResizeCorner.NONE) {
                beginResize(hoveredHandle, mx, my)
                return WindowClickAction.CONSUMED
            }

            if (mx >= x && mx <= x + width && my >= y && my <= y + titleBarHeight) {
                dragging = true
                dragOffsetX = mx - x
                dragOffsetY = my - y
                return WindowClickAction.CONSUMED
            }
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT && useSidebar && mx >= x && mx <= x + categoryWidth) {
            shownCategories.forEachIndexed { index, category ->
                val catY = categoryY(index)
                if (my < catY || my > catY + categoryHeight) return@forEachIndexed
                if (selectedCategory != category) {
                    selectedCategory = category
                    scroll.reset()
                    Style.playClickSound(1f)
                }
                return WindowClickAction.CONSUMED
            }
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT && scroll.mouseClicked(mx, my)) {
            return WindowClickAction.CONSUMED
        }

        if (isInsideContent(mx, my)) {
            visibleWidgets.forEach {
                if (it.mouseClicked(mx.toDouble(), my.toDouble(), button)) {
                    return WindowClickAction.CONSUMED
                }
            }
        }

        return WindowClickAction.CONSUMED
    }

    fun clearFocus() = settings.forEach { it.clearFocus() }

    fun mouseReleased(button: Int) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = false
            resizeCorner = ResizeCorner.NONE
            scroll.mouseReleased()
        }

        visibleWidgets.forEach { it.mouseReleased(button) }
    }

    fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        visibleWidgets.forEach {
            if (it.mouseScrolled(mouseX, mouseY, delta)) {
                return true
            }
        }

        if (contains(mouseX.toFloat(), mouseY.toFloat())) {
            if (isInsideContent(mouseX.toFloat(), mouseY.toFloat())) {
                scroll.scroll((delta * 30).toFloat())
            }
            return true
        }

        return false
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        visibleWidgets.forEach {
            if (it.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    fun charTyped(codePoint: Char): Boolean {
        visibleWidgets.forEach {
            if (it.charTyped(codePoint)) {
                return true
            }
        }
        return false
    }

    fun contains(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }

    fun cursorAt(mouseX: Float, mouseY: Float) = when {
        resizeCorner != ResizeCorner.NONE -> resizeCorner.cursor
        contains(mouseX, mouseY) -> getResizeCorner(mouseX, mouseY).cursor
        else -> null
    }

    fun clampToScreen() {
        val maxWidth = Resolution.width.coerceAtLeast(minWidth)
        val maxHeight = Resolution.height.coerceAtLeast(minHeight)

        width = width.coerceIn(minWidth, maxWidth)
        height = height.coerceIn(minHeight, maxHeight)
        x = x.coerceIn(0f, (Resolution.width - width).coerceAtLeast(0f))
        y = y.coerceIn(0f, (Resolution.height - height).coerceAtLeast(0f))
    }

    private fun updateLayout() {
        contentLeft = x + sidebarWidth + windowPadding
        contentTop = y + titleBarHeight + 8f
        contentRight = x + width - previewWidth - windowPadding
        contentBottom = y + height - bottomPadding

        previewX = x + width - previewWidth
        previewY = y + titleBarHeight
        previewHeight = height - titleBarHeight
    }

    private fun updateInteraction(mouseX: Float, mouseY: Float) {
        scroll.mouseDragged(mouseY)

        if (dragging) {
            x = (mouseX - dragOffsetX).coerceIn(0f, (Resolution.width - width).coerceAtLeast(0f))
            y = (mouseY - dragOffsetY).coerceIn(0f, (Resolution.height - height).coerceAtLeast(0f))
            return
        }

        if (resizeCorner == ResizeCorner.NONE) return

        val deltaX = mouseX - interactionStartMouseX
        val deltaY = mouseY - interactionStartMouseY

        var newX = interactionStartX
        var newY = interactionStartY
        var newWidth = interactionStartWidth
        var newHeight = interactionStartHeight

        if (resizeCorner.equalsOneOf(ResizeCorner.LEFT, ResizeCorner.TOP_LEFT, ResizeCorner.BOTTOM_LEFT)) {
            newX = (interactionStartX + deltaX).coerceIn(0f, interactionStartX + interactionStartWidth - minWidth)
            newWidth = interactionStartWidth + (interactionStartX - newX)
        }
        else if (resizeCorner.equalsOneOf(ResizeCorner.RIGHT, ResizeCorner.TOP_RIGHT, ResizeCorner.BOTTOM_RIGHT)) {
            val maxWidth = (Resolution.width - interactionStartX).coerceAtLeast(minWidth)
            newWidth = (interactionStartWidth + deltaX).coerceIn(minWidth, maxWidth)
        }

        if (resizeCorner.equalsOneOf(ResizeCorner.TOP, ResizeCorner.TOP_LEFT, ResizeCorner.TOP_RIGHT)) {
            newY = (interactionStartY + deltaY).coerceIn(0f, interactionStartY + interactionStartHeight - minHeight)
            newHeight = interactionStartHeight + (interactionStartY - newY)
        }
        else if (resizeCorner.equalsOneOf(ResizeCorner.BOTTOM, ResizeCorner.BOTTOM_LEFT, ResizeCorner.BOTTOM_RIGHT)) {
            val maxHeight = (Resolution.height - interactionStartY).coerceAtLeast(minHeight)
            newHeight = (interactionStartHeight + deltaY).coerceIn(minHeight, maxHeight)
        }

        x = newX
        y = newY
        width = newWidth
        height = newHeight
    }

    private fun beginResize(handle: ResizeCorner, mouseX: Float, mouseY: Float) {
        dragging = false
        resizeCorner = handle
        interactionStartMouseX = mouseX
        interactionStartMouseY = mouseY
        interactionStartX = x
        interactionStartY = y
        interactionStartWidth = width
        interactionStartHeight = height
    }

    private fun getResizeCorner(mouseX: Float, mouseY: Float): ResizeCorner {
        return MouseHelper.getResizeCorner(mouseX, mouseY, x, y, width, height, resizeMargin)
    }

    private fun isInsideContent(mouseX: Float, mouseY: Float): Boolean {
        return mouseX in contentLeft .. contentRight && mouseY >= contentTop && mouseY <= contentBottom
    }

    private fun isInsideCloseButton(mouseX: Float, mouseY: Float): Boolean {
        val closeX = x + width - windowPadding - closeButtonSize
        val closeY = y + ((titleBarHeight - closeButtonSize) / 2f)

        return mouseX >= closeX && mouseX <= closeX + closeButtonSize && mouseY >= closeY && mouseY <= closeY + closeButtonSize
    }

    private fun drawCloseButton(context: GuiGraphicsExtractor, hovered: Boolean) {
        val closeX = x + width - windowPadding - closeButtonSize
        val closeY = y + ((titleBarHeight - closeButtonSize) / 2f)
        val background = if (hovered) Color(180, 60, 60, 220) else Color(255, 255, 255, 18)

        context.drawRect(closeX, closeY, closeButtonSize, closeButtonSize, background)
        context.drawCenteredString("×", closeX + (closeButtonSize / 2f), closeY + 3f)
    }

    private val sidebarWidth get() = if (useSidebar) categoryWidth else 0f
    private val currentWidgets get() = if (useSidebar) selectedCategory?.widgets.orEmpty() else settings
    private val shownCategories get() = categories.filter { category -> category.widgets.any { it.config.visibility() } }

    private fun categoryY(index: Int) = y + titleBarHeight + 4f + (index * categoryHeight)

    private class Category(val name: String) {
        val widgets = mutableListOf<Widget<*>>()
    }
}