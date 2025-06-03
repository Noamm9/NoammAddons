package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.features.Feature
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.noammaddons.Companion.scope
import noammaddons.noammaddons.Companion.textRenderer
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.MathUtils.interpolateColor
import noammaddons.utils.MathUtils.lerp
import java.awt.Color
import kotlin.reflect.KProperty


class ToggleSetting(
    label: String,
    override val defaultValue: Boolean = false
): Component<Boolean>(label), Savable {
    override var height = 20.0

    override var value = defaultValue
        set(newValue) {
            if (field != newValue) {
                field = newValue
                animate()
            }
        }

    private var animProgress = if (value) 1.0 else 0.0

    private val switchTrackWidth = 25.8
    private val switchTrackHeight = 12.0
    private val switchKnobRadius = (switchTrackHeight / 2.0) - 1.5
    private val switchPaddingRight = 10.0

    private val trackColorOff = Color(23, 23, 23)
    private val knobColor = Color.WHITE

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        drawSmoothRect(compBackgroundColor, x, y, width, this.height)
        textRenderer.drawText(name, x + 6, y + 6)

        val trackX = x + width - switchPaddingRight - switchTrackWidth
        val trackY = y + (this.height - switchTrackHeight) / 2.0

        val currentTrackColor = interpolateColor(trackColorOff, accentColor, animProgress.toFloat())

        drawSmoothRect(currentTrackColor, trackX, trackY, switchTrackWidth, switchTrackHeight)

        val knobDiameter = switchKnobRadius * 2.0
        val travelDistance = switchTrackWidth - knobDiameter - (switchKnobRadius * 0.5)

        val knobCenterX = trackX + switchKnobRadius + (switchKnobRadius * 0.25) + (travelDistance * animProgress)
        val knobCenterY = trackY + switchTrackHeight / 2.0

        val knobDrawX = knobCenterX - switchKnobRadius
        val knobDrawY = knobCenterY - switchKnobRadius

        drawSmoothRect(knobColor, knobDrawX, knobDrawY, knobDiameter, knobDiameter)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        val trackX = x + width - switchPaddingRight - switchTrackWidth
        val trackY = y + (this.height - switchTrackHeight) / 2.0

        if (mouseX >= trackX && mouseX <= (trackX + switchTrackWidth) && mouseY >= trackY && mouseY <= (trackY + switchTrackHeight) && button == 0) {
            this.value = ! this.value
        }
    }

    private fun animate() = scope.launch {
        val startProgress = animProgress
        val endProgress = if (value) 1.0 else 0.0
        if (startProgress == endProgress) return@launch

        val durationSeconds = 0.20
        val startTimeNanos = System.nanoTime()

        while (true) {
            val elapsedNanos = System.nanoTime() - startTimeNanos
            val elapsedSeconds = elapsedNanos / 1_000_000_000.0
            val t = (elapsedSeconds / durationSeconds).coerceIn(0.0, 1.0)

            animProgress = lerp(startProgress, endProgress, easeOutQuad(t))

            if (t == 1.0) break
            delay(7)
        }
        animProgress = endProgress
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>): Boolean = this.value

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asBoolean?.let {
            animProgress = if (value) 1.0 else 0.0
            value = it
        }
    }
}