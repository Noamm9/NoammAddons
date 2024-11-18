package noammaddons.config.EditGui

import noammaddons.config.EditGui.components.PosElement
import noammaddons.config.EditGui.components.TextElement

object ElementsManager {
    data class HudElementData(var x: Float, var y: Float, var scale: Float)

    interface DrawableElement {
        fun draw(example: Boolean = false)

        fun isHovered(mouseX: Float, mouseY: Float): Boolean

        fun getX(): Float
        fun getY(): Float
        fun getScale(): Float

        fun setX(x: Float)
        fun setY(y: Float)
        fun setScale(scale: Float)

        fun reset()
    }

    val textElements = mutableListOf<TextElement>()
    val posElements = mutableListOf<PosElement>()
}