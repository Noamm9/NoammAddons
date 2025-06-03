package noammaddons.ui.clickgui.core

import noammaddons.utils.MouseUtils

open class AbstractElement {
    var x: Float = - 1f
    var y: Float = - 1f

    var width: Float = - 1f
    var height: Float = - 1f

    open fun draw(mouseX: Float, mouseY: Float) {}
    open fun mouseClicked(mouseX: Float, mouseY: Float, button: Int) {}
    open fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {}
    open fun mouseDragged(mouseX: Float, mouseY: Float, button: Int) {}
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return false
    }

    open fun onScroll(delta: Int) {}

    fun isHovered(mx: Float, my: Float): Boolean = MouseUtils.isElementHovered(mx, my, x, y, width, height)
}