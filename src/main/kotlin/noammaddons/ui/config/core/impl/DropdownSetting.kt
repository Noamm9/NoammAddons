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


class DropdownSetting(
    name: String,
    private val options: List<String>,
    override val defaultValue: Int = 0
): Component<Int>(name), Savable {
    override var value = defaultValue

    private var isOpen = false
    private var animProgress = 0.0
    private val optionHeight = 18.0

    override var height: Double = 20.0

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        drawSmoothRect(compBackgroundColor, x, y, width, 20.0)
        drawText("$name: ${options[value]}", x + 6, y + 6)

        val scale = if (isOpen) 2 else - 2
        drawText(
            "^",
            x + width + if (isOpen) - 18 else - 8,
            y + if (isOpen) 7 else 14,
            scale
        )

        if (isOpen || animProgress > 0.0) {
            val dropdownHeight = (options.size - 1) * optionHeight * animProgress
            drawSmoothRect(Color.WHITE, x - 1, y - 1 + 20, width + 2, dropdownHeight + 2)
            drawSmoothRect(Color(20, 20, 20), x, y + 20, width, dropdownHeight)

            var optionOffset = 0
            for ((i, option) in options.withIndex()) {
                if (i == value) continue

                val oy = y + 20 + optionOffset * optionHeight
                if (oy > y + 20 + dropdownHeight) break

                drawCenteredText(option, x + width / 2.0, oy + 5)
                optionOffset ++
            }
        }
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX in x .. (x + width) && mouseY in y .. (y + 20) && button == 0) {
            isOpen = ! isOpen
            animateDropdown()
            SoundUtils.click()
            return
        }

        if (isOpen && mouseX in x .. (x + width) && mouseY in (y + 20) .. (y + height) && button == 0) {
            val index = ((mouseY - y - 20) / optionHeight).toInt()

            var optionOffset = 0
            for (option in options.indices) {
                if (option == value) continue
                if (optionOffset == index) {
                    value = option
                    SoundUtils.click()
                    isOpen = false
                    animateDropdown()
                    break
                }
                optionOffset ++
            }
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    private fun animateDropdown() {
        scope.launch {
            val start = animProgress
            val end = if (isOpen) 1.0 else 0.0
            val duration = 0.2
            val startTime = System.nanoTime()
            while (true) {
                val elapsed = (System.nanoTime() - startTime) / 1e9
                val t = (elapsed / duration).coerceIn(0.0, 1.0)
                animProgress = lerp(start, end, easeOutQuad(t))
                height = 20.0 + animProgress * (options.size - 1) * optionHeight
                if (t == 1.0) break
                delay(7)
            }
            animProgress = end
            height = 20.0 + animProgress * (options.size - 1) * optionHeight
        }
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asInt?.let {
            value = it
        }
    }
}
