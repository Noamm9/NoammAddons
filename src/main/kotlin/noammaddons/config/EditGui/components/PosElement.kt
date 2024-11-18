package noammaddons.config.EditGui.components

import noammaddons.config.EditGui.ElementsManager.DrawableElement
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.config.EditGui.ElementsManager.posElements
import noammaddons.utils.RenderUtils
import java.awt.Color

class PosElement(
    private val dataObj: HudElementData,
    private val exampleDraw: () -> Pair<Float, Float>
): DrawableElement {

    init {
        posElements.add(this)
    }

    var width = 20f
    var height = 20f

    override fun getX() = dataObj.x
    override fun getY() = dataObj.y
    override fun getScale() = dataObj.scale

    override fun setX(x: Float) {
        dataObj.x = x
    }


    override fun setY(y: Float) {
        dataObj.y = y
    }

    override fun setScale(scale: Float) {
        dataObj.scale = scale
    }

    override fun draw(example: Boolean) {
        update()

        RenderUtils.drawRoundedRect(
            Color.RED,
            getX() - width,
            getY() - height / 2,
            width, height
        )
        exampleDraw().run {
            width = first
            height = second
        }

    }

    override fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= getX() - width && mouseX <= getX() &&
                mouseY >= getY() - height / 2 && mouseY <= getY() + height / 2
    }

    private fun update() {
        if (getScale() < 0.5f) setScale(0.5f)
    }

    override fun reset() {
        setX(200f)
        setY(200f)
        setScale(3f)
    }
}