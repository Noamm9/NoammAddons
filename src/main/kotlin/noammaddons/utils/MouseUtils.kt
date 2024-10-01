package noammaddons.utils

import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY


object MouseUtils {

    val mouseX: Float = mc.getMouseX()

    val mouseY: Float = mc.getMouseY()


    fun isElementHovered(mx: Float, my: Float, x: Double, y: Double, w: Int, h: Int): Boolean {
        return mx >= x && mx <= x + w && my >= y && my <= y + h
    }

}