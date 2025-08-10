package noammaddons.ui.config.core.impl

import com.google.gson.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.lerpColor
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.StencilUtils
import java.awt.Color
import kotlin.reflect.KProperty

class MultiCheckboxSetting(
    name: String,
    optionsWithDefaults: Map<String, Boolean>,
): Component<MutableMap<String, Boolean>>(name), Savable {

    private val options: Map<String, Boolean> = optionsWithDefaults

    override val defaultValue: MutableMap<String, Boolean> = optionsWithDefaults.toMutableMap()
    override var value: MutableMap<String, Boolean> = defaultValue.toMutableMap()

    fun get(option: String) = value[option] ?: false
    fun get(option: Int) = value.values.toList().getOrNull(option) ?: false

    private var isOpen = false
    private val mainBoxHeight = 20.0

    private var expandAnimProgress = 0.0
    private var headerHoverProgress = 0.0
    private var isHeaderHovered = false
    private val optionHoverProgress = mutableMapOf<String, Double>()
    private var currentlyHoveredOption: String? = null

    private val orderedOptions = optionsWithDefaults.keys.toList()

    private val selectionAnimProgress = mutableMapOf<String, Double>()
    private val uncheckedColor = Color(50, 50, 50)

    init {
        updateHeight()
        value.forEach { (key, isSelected) ->
            selectionAnimProgress[key] = if (isSelected) 1.0 else 0.0
        }
    }

    private fun updateHeight() {
        val contentHeight = options.size * mainBoxHeight
        height = mainBoxHeight + (contentHeight * expandAnimProgress)
    }

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val headerHovered = isMouseOverHeader(x, y, mouseX, mouseY) && ! isOpen
        if (headerHovered != isHeaderHovered) {
            isHeaderHovered = headerHovered
            animateHeaderHover(isHeaderHovered)
        }

        val headerBgColor = lerpColor(compBackgroundColor, hoverColor, headerHoverProgress)
        drawSmoothRect(headerBgColor, x, y, width, mainBoxHeight)
        textRenderer.drawText(name, x + 6, y + 6)

        val selectedCount = value.count { it.value }
        val summary = if (selectedCount == 0) "None" else "$selectedCount selected"
        textRenderer.drawText(summary, x + width - 6 - textRenderer.getStringWidth(summary), y + 6)

        if (expandAnimProgress > 0.0) {
            val contentY = y + mainBoxHeight
            val animatedContentHeight = (options.size * mainBoxHeight) * expandAnimProgress

            handleOptionHover(x, contentY, mouseX, mouseY)

            StencilUtils.beginStencilClip { drawRect(Color.WHITE, x, contentY, width, animatedContentHeight) }
            drawSmoothRect(compBackgroundColor, x, contentY, width, animatedContentHeight)

            var optionOffsetY = 0.0
            for (optionKey in orderedOptions) {
                val optionY = contentY + optionOffsetY

                val hoverProgress = optionHoverProgress.getOrDefault(optionKey, 0.0)
                if (hoverProgress > 0) {
                    val animatedHoverColor = Color(hoverColor.red, hoverColor.green, hoverColor.blue, (hoverColor.alpha * hoverProgress).toInt())
                    drawSmoothRect(animatedHoverColor, x, optionY, width, mainBoxHeight)
                }

                val boxSize = 10.0
                val boxX = x + 8
                val boxY = optionY + (mainBoxHeight - boxSize) / 2

                val selProgress = selectionAnimProgress.getOrDefault(optionKey, if (value[optionKey] == true) 1.0 else 0.0)

                val checkboxBgColor = lerpColor(uncheckedColor, accentColor, selProgress)
                drawSmoothRect(checkboxBgColor, boxX, boxY, boxSize, boxSize)

                textRenderer.drawText(optionKey, boxX + boxSize + 6, optionY + 6)
                optionOffsetY += mainBoxHeight
            }
            StencilUtils.endStencilClip()
        }
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (button != 0) return
        if (isMouseOverHeader(x, y, mouseX, mouseY)) {
            isOpen = ! isOpen
            animateExpand()
        }
        else if (isOpen && expandAnimProgress == 1.0) {
            val contentY = y + mainBoxHeight
            if (mouseX in x .. (x + width) && mouseY > contentY) {
                val optionIndex = ((mouseY - contentY) / mainBoxHeight).toInt()
                if (optionIndex in orderedOptions.indices) {
                    val clickedOptionKey = orderedOptions[optionIndex]
                    val currentIsSelected = value[clickedOptionKey] ?: false
                    val newIsSelected = ! currentIsSelected
                    value[clickedOptionKey] = newIsSelected

                    animateSelection(clickedOptionKey, newIsSelected)
                }
            }
            else {
                isOpen = false
                animateExpand()
            }
        }
    }

    private fun isMouseOverHeader(x: Double, y: Double, mouseX: Double, mouseY: Double) =
        mouseX in x .. (x + width) && mouseY in y .. (y + mainBoxHeight)

    private fun handleOptionHover(x: Double, contentY: Double, mouseX: Double, mouseY: Double) {
        var foundHoveredOption: String? = null
        if (isOpen && expandAnimProgress == 1.0) {
            if (mouseX in x .. (x + width) && mouseY > contentY) {
                val optionIndex = ((mouseY - contentY) / mainBoxHeight).toInt()
                // MODIFIED: Use the ordered list here as well.
                if (optionIndex in orderedOptions.indices) {
                    foundHoveredOption = orderedOptions[optionIndex]
                }
            }
        }

        if (foundHoveredOption != currentlyHoveredOption) {
            currentlyHoveredOption?.let { animateOptionHover(it, false) }
            foundHoveredOption?.let { animateOptionHover(it, true) }
            currentlyHoveredOption = foundHoveredOption
        }
    }

    private fun animateExpand() = scope.launch {
        val start = expandAnimProgress
        val end = if (isOpen) 1.0 else 0.0
        val duration = 250L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val t = (System.currentTimeMillis() - startTime).toDouble() / duration
            expandAnimProgress = lerp(start, end, easeOutQuad(t))
            updateHeight()
            delay(7)
        }
        expandAnimProgress = end
        updateHeight()
        if (! isOpen) currentlyHoveredOption = null
    }

    private fun animateHeaderHover(hovering: Boolean) = scope.launch {
        val start = headerHoverProgress
        val end = if (hovering) 1.0 else 0.0
        val duration = 150L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val t = (System.currentTimeMillis() - startTime).toDouble() / duration
            headerHoverProgress = lerp(start, end, easeOutQuad(t))
            delay(7)
        }
        headerHoverProgress = end
    }

    private fun animateOptionHover(option: String, hovering: Boolean) = scope.launch {
        val start = optionHoverProgress.getOrDefault(option, 0.0)
        val end = if (hovering) 1.0 else 0.0
        val duration = 150L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val t = (System.currentTimeMillis() - startTime).toDouble() / duration
            optionHoverProgress[option] = lerp(start, end, easeOutQuad(t))
            delay(7)
        }
        if (end == 0.0) optionHoverProgress.remove(option) else optionHoverProgress[option] = end
    }

    private fun animateSelection(optionKey: String, selected: Boolean) = scope.launch {
        val start = selectionAnimProgress.getOrDefault(optionKey, if (selected) 0.0 else 1.0)
        val end = if (selected) 1.0 else 0.0
        val duration = 200L

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val t = (System.currentTimeMillis() - startTime).toDouble() / duration
            selectionAnimProgress[optionKey] = lerp(start, end, easeOutQuad(t))
            delay(7)
        }
        selectionAnimProgress[optionKey] = end
    }


    override fun write(): JsonElement {
        val jsonObject = JsonObject()
        value.forEach { (k, v) -> jsonObject.add(k, JsonPrimitive(v)) }
        return jsonObject
    }

    override fun read(element: JsonElement?) {
        value = defaultValue.toMutableMap()
        if (element != null && element.isJsonObject) {
            element.asJsonObject.entrySet().forEach { (k, v) ->
                value[k] = v.asBoolean
            }
        }
        value.forEach { (key, isSelected) ->
            selectionAnimProgress[key] = if (isSelected) 1.0 else 0.0
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value
}