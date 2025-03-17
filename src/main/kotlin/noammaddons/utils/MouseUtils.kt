package noammaddons.utils

import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import org.lwjgl.input.Mouse


object MouseUtils {
    fun getMouseX(): Float {
        val mx = Mouse.getX().toFloat()
        val rw = mc.getWidth().toFloat()
        val dw = mc.displayWidth.toFloat()
        return mx * rw / dw
    }

    fun getMouseY(): Float {
        val my = Mouse.getY().toFloat()
        val rh = mc.getHeight().toFloat()
        val dh = mc.displayHeight.toFloat()
        return rh - my * rh / dh - 1f
    }


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