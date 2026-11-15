package com.github.noamm9.ui.clickgui.components

import com.mojang.blaze3d.platform.InputConstants

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.enums.WindowClickAction
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

abstract class FeatureWindow(val feature: Feature) {
    companion object {
        const val titleBarHeight = 24f
        const val windowPadding = 10f
        private const val resizeMargin = 3f
        private const val closeButtonSize = 14f
    }

    var x = 0f
        private set
    var y = 0f
        private set
    protected var width = 0f
        private set
    protected var height = 0f
        private set

    abstract val defaultWidth: Float
    abstract val defaultHeight: Float
    protected abstract val minWidth: Float
    protected abstract val minHeight: Float

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

    fun place(startX: Float, startY: Float) {
        x = startX
        y = startY
        width = defaultWidth
        height = defaultHeight
        clampToScreen()
    }

    protected abstract fun drawContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int)

    protected open fun drawContentBackground(context: GuiGraphicsExtractor) {}

    protected open fun updateLayout() {}

    protected open fun drawBackground(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, color: Color) {
        context.drawRect(x, y, width, height, color)
    }

    protected open fun onMouseClicked(mouseX: Float, mouseY: Float, button: Int) {}

    protected open fun onMouseReleased(button: Int) {}

    open fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double) = contains(mouseX.toFloat(), mouseY.toFloat())

    open fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) = false

    open fun charTyped(codePoint: Char) = false

    open fun clearFocus() {}

    open fun onClose() {}

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
        drawContentBackground(context)
        context.drawRect(x, y, width, titleBarHeight, Color(24, 24, 24, 245))
        context.drawRect(x, y, width, 2f, Style.accentColor.withAlpha(if (focused) 255 else 180))
        context.drawBorder(x, y, width, height, frameColor)

        context.drawCenteredString("§l${feature.name}", x + (width / 2f), y + 8f)
        drawCloseButton(context, closeHovered)

        drawContent(context, mouseX, mouseY)
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

        onMouseClicked(mx, my, button)
        return WindowClickAction.CONSUMED
    }

    fun mouseReleased(button: Int) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = false
            resizeCorner = ResizeCorner.NONE
        }

        onMouseReleased(button)
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

    protected open fun updateInteraction(mouseX: Float, mouseY: Float) {
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
}
