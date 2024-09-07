package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.GuiUtils.getMouseX
import NoammAddons.utils.GuiUtils.getMouseY


object MouseUtils {

    val mouseX: Float = mc.getMouseX()

    val mouseY: Float = mc.getMouseY()


    fun isElementHovered(mx: Float, my: Float, x: Double, y: Double, w: Int, h: Int): Boolean {
        return mx >= x && mx <= x + w && my >= y && my <= y + h
    }

}