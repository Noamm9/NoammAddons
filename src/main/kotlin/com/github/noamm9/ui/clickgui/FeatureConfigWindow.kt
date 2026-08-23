package com.github.noamm9.ui.clickgui

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.components.Widget
import com.github.noamm9.ui.clickgui.components.WidgetFactory
import com.github.noamm9.ui.clickgui.components.impl.CategoryWidget
import com.github.noamm9.ui.clickgui.components.impl.SeparatorWidget
import com.github.noamm9.ui.clickgui.enums.WindowClickAction
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.MouseHelper
import com.github.noamm9.ui.utils.ResizeCorner
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class FeatureConfigWindow(val feature: Feature, startX: Float, startY: Float, startWidth: Float, startHeight: Float) {
    companion object {
        private const val titleBarHeight = 24f
        private const val windowPadding = 10f
        private const val bottomPadding = 8f
        private const val resizeMargin = 3f
        private const val closeButtonSize = 14f
        private const val settingSpacing = 5f
        private const val minWidth = 180f
        private const val minHeight = 140f
    }

    var x = startX
        private set
    var y = startY
        private set
    private var width = startWidth
    private var height = startHeight

    private var visibleWidgets = emptyList<Widget<*>>()
    private val scrollAnim = Animation(200L)
    private var scrollTarget = 0f
    private var maxScroll = 0f

    val settings = buildList {
        val widgets = feature.configSettings.mapNotNull(WidgetFactory::fromSetting)
        var lastSection: String? = null

        for (widget in widgets) {
            val section = widget.config.section
            if (section != null && section != lastSection) {
                val sectionSettings = widgets.dropWhile { it !== widget }.takeWhile { it.config.section == section }
                val sectionVisibility = { sectionSettings.any { it.config.visibility() } }
                if (isNotEmpty()) add(SeparatorWidget().also { it.config.visibility = sectionVisibility })
                add(CategoryWidget(section).also { it.config.visibility = sectionVisibility })
                lastSection = section
            }
            add(widget)
        }
    }

    private var contentLeft = startX + windowPadding
    private var contentTop = startY + titleBarHeight + 8f
    private var contentRight = startX + startWidth - windowPadding
    private var contentBottom = startY + startHeight - bottomPadding

    var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    private var scrollbarDragging = false
    private var scrollbarDragStartY = 0f
    private var scrollbarDragStartScroll = 0f
    private var scrollbarX = 0f
    private var scrollbarThumbY = 0f
    private var scrollbarThumbH = 0f
    private var scrollbarViewportH = 0f

    private var resizeCorner = ResizeCorner.NONE
    private var interactionStartMouseX = 0f
    private var interactionStartMouseY = 0f
    private var interactionStartX = startX
    private var interactionStartY = startY
    private var interactionStartWidth = startWidth
    private var interactionStartHeight = startHeight

    fun render(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, focused: Boolean) {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()

        updateInteraction(mx, my)
        clampToScreen()

        val frameColor = if (focused) Color(255, 255, 255, 40) else Color(255, 255, 255, 24)
        val closeHovered = isInsideCloseButton(mx, my)

        context.drawRect(x - 2f, y - 2f, width + 4f, height + 4f, Color(0, 0, 0, 70))
        context.drawRect(x, y, width, height, Color(16, 16, 16, 235))
        context.drawRect(x, y, width, titleBarHeight, Color(24, 24, 24, 245))
        context.drawRect(x, y, width, 2f, Style.accentColor.withAlpha(if (focused) 255 else 180))
        context.drawBorder(x, y, width, height, frameColor)

        context.drawCenteredString("§l${feature.name}", x + (width / 2f), y + 8f)
        drawCloseButton(context, closeHovered)

        visibleWidgets = settings.filter { it.config.visibility.invoke() }

        contentLeft = x + windowPadding
        contentTop = y + titleBarHeight + 8f
        contentRight = x + width - windowPadding
        contentBottom = y + height - bottomPadding

        val viewportWidth = (contentRight - contentLeft).coerceAtLeast(120f)
        val viewportHeight = (contentBottom - contentTop).coerceAtLeast(40f)
        val totalContentHeight = if (visibleWidgets.isEmpty()) {
            0f
        }
        else {
            visibleWidgets.sumOf { it.height }.toFloat() + ((visibleWidgets.size - 1) * settingSpacing)
        }

        maxScroll = (totalContentHeight - viewportHeight).coerceAtLeast(0f)
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll)
        scrollAnim.update(scrollTarget)

        if (scrollAnim.value > maxScroll) {
            scrollAnim.set(maxScroll)
        }

        val scrollbarReserve = if (maxScroll > 0f) 6f else 0f
        val settingWidth = (viewportWidth - scrollbarReserve).toInt().coerceAtLeast(120)

        context.enableScissor(contentLeft.toInt(), contentTop.toInt(), contentRight.toInt(), contentBottom.toInt())

        if (visibleWidgets.isEmpty()) {
            context.drawCenteredString(
                "No visible settings",
                x + (width / 2f),
                contentTop + (viewportHeight / 2f) - 5f,
                Color.GRAY,
                shadow = false
            )
        }
        else {
            var currentY = contentTop - scrollAnim.value

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
        }

        context.disableScissor()

        if (maxScroll > 0f && totalContentHeight > 0f) {
            val barWidth = 2f
            val barX = contentRight - barWidth
            val thumbHeight = ((viewportHeight / totalContentHeight) * viewportHeight).coerceAtLeast(18f)
            val thumbTravel = (viewportHeight - thumbHeight).coerceAtLeast(0f)
            val thumbY = contentTop + ((scrollAnim.value / maxScroll) * thumbTravel)

            scrollbarX = barX
            scrollbarThumbY = thumbY
            scrollbarThumbH = thumbHeight
            scrollbarViewportH = viewportHeight

            context.drawRect(barX, contentTop, barWidth, viewportHeight, Color(255, 255, 255, 15))
            context.drawRect(barX, thumbY, barWidth, thumbHeight, Style.accentColor.withAlpha(if (focused) 180 else 140))
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): WindowClickAction {
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()

        if (button == 0 && isInsideCloseButton(mx, my)) {
            return WindowClickAction.CLOSE
        }

        if (button == 0) {
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

        if (button == 0 && maxScroll > 0f) {
            val barX = scrollbarX
            val hitWidth = 8f
            if (mx >= barX - 3f && mx <= barX + hitWidth && my >= contentTop && my <= contentBottom) {
                if (my >= scrollbarThumbY && my <= scrollbarThumbY + scrollbarThumbH) {
                    scrollbarDragging = true
                    scrollbarDragStartY = my
                    scrollbarDragStartScroll = scrollTarget
                }
                else {
                    val thumbTravel = (scrollbarViewportH - scrollbarThumbH).coerceAtLeast(0f)
                    if (thumbTravel > 0f) {
                        val ratio = ((my - contentTop - scrollbarThumbH / 2f) / thumbTravel).coerceIn(0f, 1f)
                        scrollTarget = ratio * maxScroll
                    }
                }
                return WindowClickAction.CONSUMED
            }
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

    fun mouseReleased(button: Int) {
        if (button == 0) {
            dragging = false
            resizeCorner = ResizeCorner.NONE
            scrollbarDragging = false
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
                scrollTarget = (scrollTarget - (delta * 30).toFloat()).coerceIn(0f, maxScroll)
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

    private fun updateInteraction(mouseX: Float, mouseY: Float) {
        if (scrollbarDragging) {
            val thumbTravel = (scrollbarViewportH - scrollbarThumbH).coerceAtLeast(0f)
            if (thumbTravel > 0f) {
                val delta = mouseY - scrollbarDragStartY
                val newScroll = (scrollbarDragStartScroll + (delta / thumbTravel) * maxScroll).coerceIn(0f, maxScroll)
                scrollTarget = newScroll
                scrollAnim.set(newScroll)
            }
        }

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
        context.drawCenteredString("\u00D7", closeX + (closeButtonSize / 2f), closeY + 3f)
    }
}