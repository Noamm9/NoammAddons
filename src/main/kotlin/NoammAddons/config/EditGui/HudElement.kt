package NoammAddons.config.EditGui


import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.config.EditGui.ElementsManager.HudElementData
import NoammAddons.config.EditGui.ElementsManager.elements
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.RenderUtils
import java.awt.Color
import kotlin.math.roundToInt


object ElementsManager {
    data class HudElementData(var x: Double, var y: Double, var scale: Double)
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

    var width = 20.0
    var height = 8.0

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

    fun setX(x: Double): HudElement {
        dataObj.x = x
        return this
    }

    fun setY(y: Double): HudElement {
        dataObj.y = y
        return this
    }

    fun setScale(scale: Double): HudElement {
        dataObj.scale = scale
        return this
    }

    fun draw(example: Boolean = false): HudElement {
        update()

        if (!example) {
	        RenderUtils.drawText(
		        getText(),
		        getX(),
		        getY(),
		        getScale(),
		        getColor()
	        )
        }
        else {
            RenderUtils.drawRoundedRect(
                Color(15, 15, 15, 150),
                getX(),
                getY(),
                width * getScale(),
                height * getScale()
            )

            RenderUtils.drawText(
                getText(),
                getX(),
                getY(),
                getScale(),
                getColor()
            )
        }
        return this
    }

    fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= getX() - (width * getScale() / 2) && mouseX <= getX() + (width * getScale() / 2) &&
            mouseY >= getY() && mouseY <= getY() + (height * getScale())
    }

    private fun update() {
        val lines = getText().removeFormatting().split("\n")
        width = ((lines.maxOfOrNull { mc.fontRendererObj.getStringWidth(it) } ?: 0).toDouble())

        height = ((mc.fontRendererObj.FONT_HEIGHT * lines.size).toDouble())

        if (getScale() < 0.5) setScale(0.5)
    }

    fun reset() {
        setX(100.0)
        setY(10.0 * (Math.random() * elements.size+10).roundToInt())
        setScale(1.0)
    }
}


