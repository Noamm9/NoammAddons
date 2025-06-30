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
import noammaddons.utils.StencilUtils
import java.awt.Color
import kotlin.reflect.KProperty


class DropdownSetting(
    name: String,
    val options: List<String>,
    override val defaultValue: Int = 0
): Component<Int>(name), Savable {
    override var value = defaultValue
        set(newValue) {
            if (field != newValue && newValue in options.indices) {
                field = newValue
            }
        }

    private var isOpen = false
    private var animProgress = 0.0
    private val mainBoxHeight = 20.0

    private val padding = 6
    private val dropDownPadding = 4.0

    val textColor = Color.WHITE
    val dropdownBackgroundColor = compBackgroundColor

    override var height = mainBoxHeight + if (animProgress > 0) (dropDownPadding + (options.size * mainBoxHeight)) * animProgress else 0.0

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentOptionText = if (options.isNotEmpty() && value in options.indices) options[value] else "N/A"
        val mainBgColor = if (isMouseOverMain(x, y, mouseX, mouseY) && ! isOpen) hoverColor else compBackgroundColor

        drawSmoothRect(mainBgColor, x, y, width, mainBoxHeight)

        textRenderer.drawText(name, x + padding, y + padding, textColor)
        textRenderer.drawText(currentOptionText, x + width - padding - textRenderer.getStringWidth(currentOptionText), y + padding, textColor)


        if (animProgress > 0.0) {
            val currentDropdownHeight = options.size * mainBoxHeight * animProgress
            val dropdownY = y + mainBoxHeight + dropDownPadding

            drawSmoothRect(dropdownBackgroundColor, x, dropdownY, width, currentDropdownHeight)

            val clipEndY = dropdownY + currentDropdownHeight

            StencilUtils.beginStencilClip {
                drawSmoothRect(Color.WHITE, x, dropdownY, width, clipEndY - dropdownY)
            }

            var optionOffsetY = 0.0
            for ((index, option) in options.withIndex()) {
                val optionY = dropdownY + optionOffsetY
                val isHovered = mouseX in x .. (x + width) && mouseY in optionY + 1 .. (optionY + mainBoxHeight - 2) && isOpen
                if (isHovered && animProgress == 1.0) drawSmoothRect(hoverColor, x + 1, optionY + 1, width - 2, mainBoxHeight - 2)

                if (index == value) {
                    textRenderer.drawCenteredText(
                        option,
                        x + width / 2,
                        optionY + padding,
                        accentColor
                    )
                }
                else {
                    textRenderer.drawCenteredText(
                        option,
                        x + width / 2,
                        optionY + padding,
                        textColor
                    )
                }
                optionOffsetY += mainBoxHeight
            }

            StencilUtils.endStencilClip()
        }
    }

    private fun isMouseOverMain(x: Double, y: Double, mouseX: Double, mouseY: Double): Boolean {
        return mouseX in x .. (x + width) && mouseY in y .. (y + mainBoxHeight)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (button != 0) return

        if (isMouseOverMain(x, y, mouseX, mouseY)) {
            isOpen = ! isOpen
            animateDropdown()
        }
        else if (isOpen && animProgress == 1.0) {
            val dropdownContentY = y + mainBoxHeight + dropDownPadding
            if (mouseX in x .. (x + width) && mouseY >= dropdownContentY && mouseY <= dropdownContentY + (options.size * mainBoxHeight)) {
                val clickInsideDropdownY = mouseY - dropdownContentY
                val optionIndex = (clickInsideDropdownY / mainBoxHeight).toInt()

                if (optionIndex >= 0 && optionIndex < options.size) {
                    value = optionIndex
                    isOpen = false
                    animateDropdown()
                }
            }
            else {
                if (isOpen) {
                    isOpen = false
                    animateDropdown()
                }
            }
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    private fun animateDropdown() = scope.launch {
        val startProgress = animProgress
        val endProgress = if (isOpen) 1.0 else 0.0
        val animationDuration = 250L

        val startTime = System.currentTimeMillis()
        var elapsedTime: Long

        while (System.currentTimeMillis() - startTime < animationDuration) {
            elapsedTime = System.currentTimeMillis() - startTime
            val t = elapsedTime.toDouble() / animationDuration
            animProgress = lerp(startProgress, endProgress, easeOutQuad(t))
            delay(7)
            height = mainBoxHeight + if (animProgress > 0) (dropDownPadding + (options.size * mainBoxHeight)) * animProgress else 0.0
        }
        animProgress = endProgress
        height = mainBoxHeight + if (animProgress > 0) (dropDownPadding + (options.size * mainBoxHeight)) * animProgress else 0.0
    }

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asInt?.let {
            value = if (it in options.indices) it
            else defaultValue.coerceIn(options.indices)
        }
    }
}