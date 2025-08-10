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
    private var dropdownAnimProgress = 0.0
    private var hoverAnimProgress = 0.0
    private var isHovered = false

    private var optionHoverProgress = mutableMapOf<Int, Double>()
    private var currentlyHoveredOption: Int? = null

    private val mainBoxHeight = 20.0
    private val padding = 6.0
    private val dropDownPadding = 4.0

    val textColor = Color.WHITE
    val dropdownBackgroundColor = compBackgroundColor

    override var height = mainBoxHeight + if (dropdownAnimProgress > 0) (dropDownPadding + (options.size * mainBoxHeight)) * dropdownAnimProgress else 0.0

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentlyHovered = isMouseOverMain(x, y, mouseX, mouseY) && ! isOpen
        if (currentlyHovered != isHovered) {
            isHovered = currentlyHovered
            animateHover(isHovered)
        }

        val currentOptionText = if (options.isNotEmpty() && value in options.indices) options[value] else "N/A"
        val mainBgColor = lerpColor(compBackgroundColor, hoverColor, hoverAnimProgress)

        drawSmoothRect(mainBgColor, x, y, width, mainBoxHeight)

        textRenderer.drawText(name, x + padding, y + padding, textColor)
        textRenderer.drawText(currentOptionText, x + width - padding - textRenderer.getStringWidth(currentOptionText), y + padding, textColor)

        if (dropdownAnimProgress > 0.0) {
            val dropdownY = y + mainBoxHeight + dropDownPadding
            val currentDropdownHeight = options.size * mainBoxHeight * dropdownAnimProgress

            drawSmoothRect(dropdownBackgroundColor, x, dropdownY, width, currentDropdownHeight)

            var foundHoveredOption: Int? = null
            if (isOpen && dropdownAnimProgress == 1.0) {
                val dropdownContentY = y + mainBoxHeight + dropDownPadding
                if (mouseX in x .. (x + width) && mouseY >= dropdownContentY && mouseY <= dropdownContentY + (options.size * mainBoxHeight)) {
                    val localY = mouseY - dropdownContentY
                    val index = (localY / mainBoxHeight).toInt()
                    if (index in options.indices) {
                        foundHoveredOption = index
                    }
                }
            }

            if (foundHoveredOption != currentlyHoveredOption) {
                currentlyHoveredOption?.let { animateOptionHover(it, false) }
                foundHoveredOption?.let { animateOptionHover(it, true) }
                currentlyHoveredOption = foundHoveredOption
            }

            val clipEndY = dropdownY + currentDropdownHeight
            StencilUtils.beginStencilClip {
                drawSmoothRect(Color.WHITE, x, dropdownY, width, clipEndY - dropdownY)
            }

            var optionOffsetY = 0.0
            for ((index, option) in options.withIndex()) {
                val optionY = dropdownY + optionOffsetY
                val hoverProgress = optionHoverProgress.getOrDefault(index, 0.0)

                if (hoverProgress > 0.0) {
                    val animatedHoverColor = Color(hoverColor.red, hoverColor.green, hoverColor.blue, (hoverColor.alpha * hoverProgress).toInt())
                    if (animatedHoverColor.alpha > 1) drawSmoothRect(animatedHoverColor, x + 1, optionY + 1, width - 2, mainBoxHeight - 2)
                }

                val color = if (index == value) accentColor else textColor
                textRenderer.drawCenteredText(option, x + width / 2, optionY + padding, color)
                optionOffsetY += mainBoxHeight
            }

            StencilUtils.endStencilClip()
        }
        else if (currentlyHoveredOption != null) {
            currentlyHoveredOption?.let { animateOptionHover(it, false) }
            currentlyHoveredOption = null
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
        else if (isOpen && dropdownAnimProgress == 1.0) {
            val dropdownContentY = y + mainBoxHeight + dropDownPadding
            if (mouseX in x .. (x + width) && mouseY >= dropdownContentY && mouseY <= dropdownContentY + (options.size * mainBoxHeight)) {
                val clickInsideDropdownY = mouseY - dropdownContentY
                val optionIndex = (clickInsideDropdownY / mainBoxHeight).toInt()
                if (optionIndex in options.indices) {
                    value = optionIndex
                    isOpen = false
                    animateDropdown()
                }
            }
            else {
                isOpen = false
                animateDropdown()
            }
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    private fun animateOptionHover(index: Int, hovering: Boolean) = scope.launch {
        val startProgress = optionHoverProgress.getOrDefault(index, 0.0)
        val endProgress = if (hovering) 1.0 else 0.0
        val animationDuration = 100L

        val startTime = System.currentTimeMillis()
        var elapsedTime: Long

        while (System.currentTimeMillis() - startTime < animationDuration) {
            elapsedTime = System.currentTimeMillis() - startTime
            val t = elapsedTime.toDouble() / animationDuration
            optionHoverProgress[index] = lerp(startProgress, endProgress, easeOutQuad(t))
            delay(7)
        }

        if (endProgress == 0.0) optionHoverProgress.remove(index)
        else optionHoverProgress[index] = endProgress

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

    private fun animateDropdown() = scope.launch {
        val startProgress = dropdownAnimProgress
        val endProgress = if (isOpen) 1.0 else 0.0
        val animationDuration = 250L

        val startTime = System.currentTimeMillis()
        var elapsedTime: Long

        while (System.currentTimeMillis() - startTime < animationDuration) {
            elapsedTime = System.currentTimeMillis() - startTime
            val t = elapsedTime.toDouble() / animationDuration
            dropdownAnimProgress = lerp(startProgress, endProgress, easeOutQuad(t))
            delay(7)
            height = mainBoxHeight + if (dropdownAnimProgress > 0) (dropDownPadding + (options.size * mainBoxHeight)) * dropdownAnimProgress else 0.0
        }
        dropdownAnimProgress = endProgress
        height = mainBoxHeight + if (dropdownAnimProgress > 0) (dropDownPadding + (options.size * mainBoxHeight)) * dropdownAnimProgress else 0.0
    }

    override fun write(): JsonElement = JsonPrimitive(value)

    override fun read(element: JsonElement?) {
        element?.asInt?.let {
            value = if (it in options.indices) it else defaultValue.coerceIn(options.indices)
        }
    }
}