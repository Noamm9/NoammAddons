package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import noammaddons.features.Feature
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils
import java.awt.Color
import kotlin.reflect.KProperty

class SliderSetting(
    label: String,
    val min: Number,
    val max: Number,
    override val defaultValue: Number
): Component<Number>(label), Savable {
    override var value: Number = defaultValue

    private val sliderHeight = 6.0
    private val thumbRadius = 5.0
    private val padding = 6.0

    override val height: Double = 30.0

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        drawSmoothRect(compBackgroundColor, x, y, width, height)

        val valueStr = String.format("%.1f", value.toDouble())

        drawText(name, x + padding, y + padding)
        drawText(valueStr, x + width - padding - getStringWidth(valueStr), y + padding)

        val sliderY = y + height - sliderHeight - padding
        val trackColor = Color(46, 46, 46)

        drawRoundedRect(trackColor, x + padding, sliderY, width - padding * 2, sliderHeight, 3f)

        val percent = ((value.toDouble() - min.toDouble()) / (max.toDouble() - min.toDouble())).coerceIn(.0, 1.0)
        val fillWidth = (width - padding * 2) * percent
        drawRoundedRect(accentColor, x + padding, sliderY, fillWidth, sliderHeight, 3f)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (button != 0) return
        val sliderY = y + height - sliderHeight - padding
        if (mouseX in x + padding .. x + width - padding && mouseY in sliderY .. sliderY + sliderHeight + thumbRadius * 2) {
            updateValue(mouseX, x + padding)
            SoundUtils.click()
        }
    }

    override fun mouseDragged(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        val sliderY = y + height - sliderHeight - padding
        if (mouseX in x + padding .. x + width - padding && mouseY in sliderY .. sliderY + sliderHeight + thumbRadius * 2) {
            updateValue(mouseX, x + padding)
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    private fun updateValue(mouseX: Double, sliderStartX: Double) {
        val usableWidth = width - padding * 2
        val percent = ((mouseX - sliderStartX) / usableWidth).coerceIn(0.0, 1.0)
        value = min.toDouble() + (max.toDouble() - min.toDouble()) * percent
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asDouble?.let {
            value = it
        }
    }
}

