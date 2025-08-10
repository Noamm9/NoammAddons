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
                animateToggle()
            }
        }

    private var toggleAnimProgress = if (value) 1.0 else 0.0

    private var hoverAnimProgress = 0.0
    private var isHovered = false

    private val switchTrackWidth = 25.8
    private val switchTrackHeight = 12.0
    private val switchKnobRadius = (switchTrackHeight / 2.0) - 1.5
    private val switchPaddingRight = 10.0

    private val trackColorOff = Color(23, 23, 23)
    private val knobColor = Color.WHITE

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentlyHovered = isMouseOver(x, y, mouseX, mouseY)
        if (currentlyHovered != isHovered) {
            isHovered = currentlyHovered
            animateHover(isHovered)
        }

        val animatedBgColor = lerpColor(compBackgroundColor, hoverColor, hoverAnimProgress)
        drawSmoothRect(animatedBgColor, x, y, width, this.height)

        textRenderer.drawText(name, x + 6, y + 6)

        val trackX = x + width - switchPaddingRight - switchTrackWidth
        val trackY = y + (this.height - switchTrackHeight) / 2.0

        val currentTrackColor = lerpColor(trackColorOff, accentColor, toggleAnimProgress)
        drawSmoothRect(currentTrackColor, trackX, trackY, switchTrackWidth, switchTrackHeight)

        val knobDiameter = switchKnobRadius * 2.0
        val travelDistance = switchTrackWidth - knobDiameter - (switchKnobRadius * 0.5)

        val knobCenterX = trackX + switchKnobRadius + (switchKnobRadius * 0.25) + (travelDistance * toggleAnimProgress)
        val knobCenterY = trackY + switchTrackHeight / 2.0

        val knobDrawX = knobCenterX - switchKnobRadius
        val knobDrawY = knobCenterY - switchKnobRadius

        drawSmoothRect(knobColor, knobDrawX, knobDrawY, knobDiameter, knobDiameter)
    }

    private fun isMouseOver(x: Double, y: Double, mouseX: Double, mouseY: Double): Boolean {
        return mouseX in x .. (x + width) && mouseY in y .. (y + height)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (isMouseOver(x, y, mouseX, mouseY) && button == 0) {
            this.value = ! this.value
        }
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

    private fun animateToggle() = scope.launch {
        val startProgress = toggleAnimProgress
        val endProgress = if (value) 1.0 else 0.0
        if (startProgress == endProgress) return@launch

        val durationSeconds = 0.20
        val startTimeNanos = System.nanoTime()

        while (true) {
            val elapsedSeconds = (System.nanoTime() - startTimeNanos) / 1e9
            if (elapsedSeconds >= durationSeconds) break
            val t = elapsedSeconds / durationSeconds

            toggleAnimProgress = lerp(startProgress, endProgress, easeOutQuad(t))
            delay(7)
        }
        toggleAnimProgress = endProgress
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>): Boolean = this.value

    override fun write(): JsonElement = JsonPrimitive(value)

    override fun read(element: JsonElement?) {
        element?.asBoolean?.let { newValue ->
            toggleAnimProgress = if (value) 1.0 else 0.0
            value = newValue
        }
    }
}