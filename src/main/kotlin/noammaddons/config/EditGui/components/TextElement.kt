package noammaddons.config.EditGui.components

import gg.essential.universal.UGraphics.getStringWidth
import net.minecraft.client.renderer.GlStateManager
import noammaddons.config.EditGui.ElementsManager.DrawableElement
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.config.EditGui.ElementsManager.posElements
import noammaddons.config.EditGui.ElementsManager.textElements
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawText
import java.awt.Color
import kotlin.math.roundToInt

class TextElement(
    private var textString: String = "",
    private var color: Color = Color.WHITE,
    private val dataObj: HudElementData
): DrawableElement {

    init {
        textString = textString.addColor()
        textElements.add(this)
    }

    var width = 20f
    var height = 8f

    fun getText() = textString
    fun getColor() = color
    override fun getX() = dataObj.x
    override fun getY() = dataObj.y
    override fun getScale() = dataObj.scale

    fun setText(text: String) {
        textString = text.addColor()
    }

    fun setColor(newColor: Color) {
        color = newColor
    }

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
        GlStateManager.pushMatrix()

        if (! example) {
            drawText(
                getText(),
                getX(),
                getY(),
                getScale(),
                getColor(),
            )
        }
        else {
            RenderUtils.drawRoundedRect(
                Color(15, 15, 15, 150),
                getX(), getY(),
                width * getScale(),
                height * getScale()
            )

            drawText(
                getText(),
                getX(),
                getY(),
                getScale(),
                getColor(),
            )
        }
        GlStateManager.popMatrix()
    }

    override fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= getX() - width * getScale() && mouseX <= getX() + width * getScale() &&
                mouseY >= getY() && mouseY <= getY() + height * getScale()
    }

    private fun update() {
        val lines = getText().removeFormatting().split("\n")
        width = ((lines.maxOfOrNull { getStringWidth(it) } ?: 0f).toFloat())

        height = (mc.fontRendererObj.FONT_HEIGHT * lines.size).toFloat()

        if (getScale() < 0.5f) setScale(0.5f)
    }

    override fun reset() {
        setX(100f)
        setY(10f * (Math.random() * ((textElements + posElements).size + 10)).roundToInt())
        setScale(1f)
    }
}


