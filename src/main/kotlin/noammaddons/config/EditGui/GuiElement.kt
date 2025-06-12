package noammaddons.config.EditGui

import noammaddons.utils.DataClasses
import noammaddons.utils.MouseUtils


abstract class GuiElement(val dataObj: DataClasses.HudElementData) {
    init {
        HudEditorScreen.elements.add(this)
    }

    abstract val enabled: Boolean
    abstract val width: Float
    abstract val height: Float

    open fun getX() = dataObj.x
    open fun getY() = dataObj.y
    open fun getScale() = dataObj.scale

    open fun setX(x: Float) {
        dataObj.x = x
    }

    open fun setY(y: Float) {
        dataObj.y = y
    }

    open fun setScale(scale: Float) {
        dataObj.scale = scale
    }

    abstract fun draw()

    open fun exampleDraw() = this.draw()

    open fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        return MouseUtils.isElementHovered(
            mouseX, mouseY,
            getX(), getY(),
            width * getScale(),
            height * getScale()
        )
    }

    open fun reset() {
        setX(200f)
        setY(200f)
        setScale(1f)
    }
}