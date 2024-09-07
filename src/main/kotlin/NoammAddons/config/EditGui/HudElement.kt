package NoammAddons.config.EditGui


import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.config.EditGui.ElementsManager.addElement
import NoammAddons.config.EditGui.ElementsManager.HudElementData
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.RenderUtils
import java.awt.Color



object ElementsManager {
    data class HudElementData(var x: Double, var y: Double, var scale: Double)

    val elements = mutableListOf<HudElement>()

    fun addElement(element: HudElement) {
        elements.add(element)
    }
}



class HudElement(private var textString: String = "", private var color: Color = Color.WHITE, private val dataObj: HudElementData) {

    init {
        addElement(this)
    }

    var width = 20.0
    var height = 8.0

    fun getText() = textString
    fun getColor() = color
    fun getX() = dataObj.x
    fun getY() = dataObj.y
    fun getScale() = dataObj.scale

    fun setText(text: String): HudElement {
        textString = text
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
        Update()

        if (!example) {
            RenderUtils.drawText(textString.addColor(), dataObj.x, dataObj.y, dataObj.scale, color)
        }

        if (example) {
            RenderUtils.drawRoundedRect(
                Color(15, 15, 15, 150),
                dataObj.x, dataObj.y,
                width, height
            )

            RenderUtils.drawText(textString.addColor(), dataObj.x, dataObj.y, dataObj.scale, color)
        }
        return this
    }

    fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= getX() && mouseX <= getX() + width &&
                mouseY >= getY() && mouseY <= getY() + height
    }

    private fun Update() {
        width = (mc.fontRendererObj.getStringWidth(textString.removeFormatting()) * getScale())
        height = if (textString.contains("\n"))
            ((mc.fontRendererObj.FONT_HEIGHT * (textString.removeFormatting().split("\n").size +1)) * getScale())
        else (mc.fontRendererObj.FONT_HEIGHT * getScale())

        if (getScale() < 0.5) setScale(0.5)

    }

    fun reset() {
        dataObj.x = 10.0
        dataObj.y = 10.0 * (Math.random()*10)
        dataObj.scale = 2.0
    }
}

