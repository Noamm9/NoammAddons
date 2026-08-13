package com.github.noamm9.ui.utils

import com.github.noamm9.NoammAddons
import org.lwjgl.glfw.GLFW

object MouseHelper {
    private val cursorCache = mutableMapOf<Int, Long>()

    fun setCursor(shape: Int) {
        val cursor = cursorCache.getOrPut(shape) { GLFW.glfwCreateStandardCursor(shape) }
        GLFW.glfwSetCursor(NoammAddons.mc.window.handle(), cursor)
    }

    fun resetCursor() = setCursor(GLFW.GLFW_ARROW_CURSOR)

    fun getResizeCorner(mx: Number, my: Number, x: Number, y: Number, w: Number, h: Number, m: Number = 2): ResizeCorner {
        val mx = mx.toFloat()
        val my = my.toFloat()
        val x = x.toFloat()
        val y = y.toFloat()
        val w = w.toFloat()
        val h = h.toFloat()
        val m = m.toFloat()

        if (mx !in x .. (x + w) || my !in y .. (y + h)) return ResizeCorner.NONE

        val onLeft = mx in x .. (x + m)
        val onRight = mx in (x + w - m) .. (x + w)
        val onTop = my in y .. (y + m)
        val onBottom = my in (y + h - m) .. (y + h)

        return when {
            onLeft && onTop -> ResizeCorner.TOP_LEFT
            onRight && onTop -> ResizeCorner.TOP_RIGHT
            onLeft && onBottom -> ResizeCorner.BOTTOM_LEFT
            onRight && onBottom -> ResizeCorner.BOTTOM_RIGHT
            onLeft -> ResizeCorner.LEFT
            onRight -> ResizeCorner.RIGHT
            onTop -> ResizeCorner.TOP
            onBottom -> ResizeCorner.BOTTOM
            else -> ResizeCorner.NONE
        }
    }
}