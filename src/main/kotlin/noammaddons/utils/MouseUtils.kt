package noammaddons.utils

import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY


object MouseUtils {
    @JvmStatic
    val mouseX: Float get() = mc.getMouseX()

    @JvmStatic
    val mouseY: Float get() = mc.getMouseY()

    @JvmStatic
    @Suppress("NAME_SHADOWING")
    fun isElementHovered(mx: Number, my: Number, x: Number, y: Number, w: Number, h: Number): Boolean {
        val mx = mx.toLong()
        val my = my.toLong()
        val x = x.toLong()
        val y = y.toLong()
        val w = w.toLong()
        val h = h.toLong()

        return mx >= x && mx <= x + w && my >= y && my <= y + h
    }
}