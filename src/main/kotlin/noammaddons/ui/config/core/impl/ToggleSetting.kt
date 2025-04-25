package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.scope
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.MathUtils.interpolateColor
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils
import kotlin.reflect.KProperty


class ToggleSetting(label: String, override val defaultValue: Boolean = false): Component<Boolean>(label), Savable {
    override var value = defaultValue
    private var animProgress = if (value) 1.0 else 0.0  // 0.0 = off, 1.0 = on

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val trackHeight = 10.0
        val trackWidth = width / 8.0
        val trackX = x + width - trackWidth - 10
        val trackY = y + (height - trackHeight) / 2
        val trackColor = interpolateColor(hoverColor, accentColor, animProgress.toFloat())

        drawSmoothRect(compBackgroundColor, x, y, width, height)
        drawSmoothRect(trackColor, trackX, trackY, trackWidth, trackHeight)
        drawText(name, x + 6, y + 6)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX in x .. (x + width) && mouseY in y .. (y + height) && button == 0) {
            SoundUtils.click()
            value = ! value
            animate()
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>): Boolean = value

    private fun animate() {
        scope.launch {
            val start = animProgress
            val end = if (value) 1.0 else 0.0
            val duration = 0.15  // seconds
            val startTime = System.nanoTime()
            while (true) {
                val elapsed = (System.nanoTime() - startTime) / 1e9
                val t = (elapsed / duration).coerceIn(0.0, 1.0)
                animProgress = lerp(start, end, easeOutQuad(t))
                if (t == 1.0) break
                delay(7)
            }
            animProgress = end
        }
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asBoolean?.let {
            value = it
            animProgress = if (value) 1.0 else 0.0
        }
    }
}
