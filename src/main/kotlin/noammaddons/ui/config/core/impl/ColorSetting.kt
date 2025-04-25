package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.scope
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils
import java.awt.Color
import kotlin.reflect.KProperty


class ColorSetting(
    label: String,
    override var defaultValue: Color,
    val withAlpha: Boolean = true
): Component<Color>(label), Savable {
    override var value = defaultValue

    private var expanded = false
    private var animProgress = 0.0
    private var draggingChannel: Int? = null
    private val sliderWidth = 140
    private val collapsedHeight = 22.0
    private val expandedHeight = if (withAlpha) 96.0 else 77.0

    override var height = collapsedHeight

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        drawSmoothRect(compBackgroundColor, x, y, width, height)
        drawText(name, x + 6, y + 7)


        val previewX = x + width - 20
        val previewY = y + 4
        drawSmoothRect(value, previewX, previewY, 14.0, 14.0)

        if (animProgress <= 0.0) return

        val components = mutableListOf(
            Triple("R", value.red, Color.RED),
            Triple("G", value.green, Color.GREEN),
            Triple("B", value.blue, Color.BLUE)
        )
        if (withAlpha) components.add(Triple("A", value.alpha, Color.WHITE))

        val offsetY = y + 24
        for ((index, triple) in components.withIndex()) {
            val (label, value, fillColor) = triple
            val sliderY = offsetY + index * 18
            if (sliderY + 8 > y + height) break
            drawText("$label:", x + 6, sliderY + 1)
            drawSmoothRect(hoverColor, x + 20, sliderY, sliderWidth, 8.0)
            drawSmoothRect(fillColor, x + 20, sliderY, sliderWidth * (value / 255.0), 8.0)
            drawCenteredText("$value", x + 20 + sliderWidth / 2, sliderY - 1)
        }
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX in x .. (x + width) && mouseY in y .. (y + collapsedHeight)) {
            expanded = ! expanded
            animateExpand()
            SoundUtils.click()
            return
        }

        if (! expanded || button != 0) return

        val offsetY = y + 24
        val indices = (0 .. 2).toMutableList().apply { if (withAlpha) add(3) }

        val checkClick = { index: Int ->
            val sliderY = offsetY + index * 18
            mouseX in (x + 20) .. (x + 20 + sliderWidth) && mouseY in sliderY .. (sliderY + 8.0)
        }

        for (i in indices) if (checkClick(i)) draggingChannel = i
    }

    override fun mouseDragged(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (! expanded || draggingChannel == null) return

        val color = value
        val percent = ((mouseX - (x + 20)) / sliderWidth).coerceIn(0.0, 1.0)
        val newValue = (percent * 255).toInt()

        value = when (draggingChannel) {
            0 -> Color(newValue, color.green, color.blue, color.alpha)
            1 -> Color(color.red, newValue, color.blue, color.alpha)
            2 -> Color(color.red, color.green, newValue, color.alpha)
            3 -> Color(color.red, color.green, color.blue, newValue)
            else -> value
        }
    }

    override fun mouseRelease(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        draggingChannel = null
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    private fun animateExpand() {
        scope.launch {
            val start = animProgress
            val end = if (expanded) 1.0 else 0.0
            val duration = 0.2
            val startTime = System.nanoTime()
            while (true) {
                val elapsed = (System.nanoTime() - startTime) / 1e9
                val t = (elapsed / duration).coerceIn(0.0, 1.0)
                animProgress = lerp(start, end, easeOutQuad(t))
                height = collapsedHeight + animProgress * (expandedHeight - collapsedHeight)
                if (t == 1.0) break
                delay(7)
            }
            animProgress = end
            height = collapsedHeight + animProgress * (expandedHeight - collapsedHeight)
        }
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value.rgb)
    }

    override fun read(element: JsonElement?) {
        element?.asInt?.let {
            value = Color(it, withAlpha)
        }
    }
}
