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
    fun isElementHovered(mx: Number, my: Number, x: Number, y: Number, w: Number, h: Number): Boolean {
        val (mxD, myD) = mx.toDouble() to my.toDouble()
        val (xD, yD) = x.toDouble() to y.toDouble()
        val (wD, hD) = w.toDouble() to h.toDouble()
        return mxD >= xD && mxD <= xD + wD && myD >= yD && myD <= yD + hD
    }

    fun onMouseEnter(mx: Number, my: Number, x: Number, y: Number, w: Number, h: Number, block: () -> Unit) {
        if (! isElementHovered(mx, my, x, y, w, h)) return
        block()
    }
}