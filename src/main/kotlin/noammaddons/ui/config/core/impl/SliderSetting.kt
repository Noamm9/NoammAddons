package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.lerpColor
import java.awt.Color
import kotlin.math.round
import kotlin.reflect.KProperty

class SliderSetting<T: Number>(
    label: String,
    val min: T,
    val max: T,
    val increment: T,
    override val defaultValue: T,
): Component<T>(label), Savable {
    override var value: T = snapToStep(defaultValue)
    override var height: Double = 30.0

    private val sliderHeight = 6.0
    private val sliderInteractionPaddingY = 5.0
    private val padding = 6.0
    private var dragging = false

    private var hoverAnimProgress = 0.0
    private var isHovered = false

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentlyHovered = isMouseOver(x, y, mouseX, mouseY) && ! dragging
        if (currentlyHovered != isHovered) {
            isHovered = currentlyHovered
            animateHover(isHovered)
        }

        val animatedBgColor = lerpColor(compBackgroundColor, hoverColor, hoverAnimProgress)
        drawSmoothRect(animatedBgColor, x, y, width, height)

        val valueStr = stringfy(value)
        textRenderer.drawText(name, x + padding, y + padding)
        textRenderer.drawText(valueStr, x + width - padding - textRenderer.getStringWidth(valueStr), y + padding)

        val sliderY = y + height - sliderHeight - padding
        drawSmoothRect(Color(46, 46, 46), x + padding, sliderY, width - padding * 2, sliderHeight)

        val percent = ((value.toDouble() - min.toDouble()) / (max.toDouble() - min.toDouble())).coerceIn(0.0, 1.0)
        val fillWidth = (width - padding * 2) * percent
        drawSmoothRect(accentColor, x + padding, sliderY, fillWidth, sliderHeight)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (button != 0) return
        if (isMouseOverSlider(x, y, mouseX, mouseY)) {
            dragging = true
            updateValue(mouseX, x + padding)
        }
    }

    override fun mouseDragged(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (button != 0) return
        if (dragging) {
            updateValue(mouseX, x + padding)
        }
    }

    override fun mouseRelease(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (button != 0 || ! dragging) return
        dragging = false
    }

    private fun isMouseOver(x: Double, y: Double, mouseX: Double, mouseY: Double): Boolean {
        return mouseX in x .. (x + width) && mouseY in y .. (y + height)
    }

    private fun isMouseOverSlider(actualX: Double, actualY: Double, mouseX: Double, mouseY: Double): Boolean {
        val sliderVisualY = actualY + height - sliderHeight - padding
        val sliderTopBound = sliderVisualY - sliderInteractionPaddingY
        val sliderBottomBound = sliderVisualY + sliderHeight + sliderInteractionPaddingY
        val sliderLeftBound = actualX + padding
        val sliderRightBound = actualX + width - padding
        return mouseX in sliderLeftBound .. sliderRightBound && mouseY in sliderTopBound .. sliderBottomBound
    }

    private fun animateHover(hovering: Boolean) = scope.launch {
        val startProgress = hoverAnimProgress
        val endProgress = if (hovering) 1.0 else 0.0
        val animationDuration = 150L

        val startTime = System.currentTimeMillis()
        var elapsedTime: Long

        while (System.currentTimeMillis() - startTime < animationDuration) {
            elapsedTime = System.currentTimeMillis() - startTime
            val t = elapsedTime.toDouble() / animationDuration
            hoverAnimProgress = lerp(startProgress, endProgress, easeOutQuad(t))
            delay(7)
        }
        hoverAnimProgress = endProgress
    }

    private fun snapToStep(rawValue: T): T {
        val rawDouble = rawValue.toDouble()
        val minD = min.toDouble()
        val maxD = max.toDouble()
        val stepD = increment.toDouble()

        if (stepD <= 0) return rawDouble.coerceIn(minD, maxD).convertToType()

        val steps = round((rawDouble - minD) / stepD)
        var steppedValue = minD + (steps * stepD)
        steppedValue = steppedValue.coerceIn(minD, maxD)
        return steppedValue.convertToType()
    }

    private fun Number.convertToType(): T {
        @Suppress("UNCHECKED_CAST")
        return when (defaultValue) {
            is Int -> toInt() as T
            is Long -> toLong() as T
            is Float -> toFloat() as T
            is Double -> toDouble() as T
            else -> this as T
        }
    }

    private fun stringfy(value: T) = when (value) {
        is Int, is Long -> String.format("%d", value.toLong())
        else -> {
            val stepD = increment.toDouble()
            if (stepD.rem(1.0) == 0.0 && value.toDouble().rem(1.0) == 0.0) String.format("%.0f", value.toDouble())
            else {
                val stepStr = stepD.toString()
                val decimalPlaces = stepStr.substringAfter('.', "").length
                when {
                    decimalPlaces in 1 .. 2 -> String.format("%.${decimalPlaces}f", value.toDouble())
                    stepD < 1.0 && stepD > 0.0 -> String.format("%.2f", value.toDouble())
                    else -> String.format("%.1f", value.toDouble())
                }
            }
        }
    }

    private fun updateValue(mouseX: Double, sliderStartX: Double) {
        val usableWidth = (width - padding * 2).takeIf { it > 0 } ?: return
        val percent = ((mouseX - sliderStartX) / usableWidth).coerceIn(0.0, 1.0)
        val rawValue = min.toDouble() + (max.toDouble() - min.toDouble()) * percent
        this.value = snapToStep(rawValue.convertToType())
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value
    override fun write(): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement?) {
        element?.asNumber?.let { value = snapToStep(it.convertToType()) }
    }
}