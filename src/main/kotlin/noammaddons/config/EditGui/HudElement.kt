package noammaddons.config.EditGui


import net.minecraft.client.renderer.GlStateManager
import noammaddons.config.EditGui.ElementsManager.HudElementData
import noammaddons.config.EditGui.ElementsManager.elements
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawText
import java.awt.Color
import kotlin.math.roundToInt


object ElementsManager {
    data class HudElementData(var x: Float, var y: Float, var scale: Float)
    val elements = mutableListOf<HudElement>()
}



class HudElement(
    private var textString: String = "",
    private var color: Color = Color.WHITE,
    private val dataObj: HudElementData
) {

    init {
        textString = textString.addColor()
        elements.add(this)
    }

    var width = 20f
    var height = 8f

    fun getText() = textString
    fun getColor() = color
    fun getX() = dataObj.x
    fun getY() = dataObj.y
    fun getScale() = dataObj.scale

    fun setText(text: String): HudElement {
        textString = text.addColor()
        return this
    }

    fun setColor(newColor: Color): HudElement {
        color = newColor
        return this
    }

    fun setX(x: Float): HudElement {
        dataObj.x = x
        return this
    }

    fun setY(y: Float): HudElement {
        dataObj.y = y
        return this
    }

    fun setScale(scale: Float): HudElement {
        dataObj.scale = scale
        return this
    }

    fun draw(example: Boolean = false): HudElement {
        update()
		GlStateManager.pushMatrix()
	    
        if (!example) {
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
        return this
    }

    fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= getX() - width * getScale() && mouseX <= getX() + width * getScale() &&
            mouseY >= getY() && mouseY <= getY() + height * getScale()
    }

    private fun update() {
        val lines = getText().removeFormatting().split("\n")
        width = ((lines.maxOfOrNull { mc.fontRendererObj.getStringWidth(it) } ?: 0f).toFloat())

        height = (mc.fontRendererObj.FONT_HEIGHT * lines.size).toFloat()

        if (getScale() < 0.5f) setScale(0.5f)
    }

    fun reset() {
        setX(100f)
        setY(10f * (Math.random() * (elements.size+10)).roundToInt())
        setScale(1f)
    }
}


