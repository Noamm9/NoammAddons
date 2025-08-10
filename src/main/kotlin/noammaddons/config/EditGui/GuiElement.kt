package noammaddons.config.EditGui

import net.minecraft.client.renderer.GlStateManager
import noammaddons.utils.DataClasses
import noammaddons.utils.MouseUtils
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawRectBorder
import java.awt.Color


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
        return MouseUtils.isMouseOver(
            mouseX, mouseY,
            getX(), getY(),
            width * getScale(),
            height * getScale()
        )
    }

    open fun reset() {
        setX(50f)
        setY(50f)
        setScale(1f)
    }

    open fun renderBackground(isHovered: Boolean) {
        val alpha = if (isHovered) 140 else 90
        val borderColor = when {
            isHovered -> Color(100, 180, 255)
            else -> Color(100, 100, 120)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(getX(), getY(), 0f)
        GlStateManager.scale(getScale(), getScale(), getScale())
        drawRect(Color(30, 35, 45, alpha), - 2, - 2, width + 4, height + 4)
        drawRectBorder(borderColor, - 2, - 2, width + 4, height + 4)
        GlStateManager.popMatrix()
    }
}