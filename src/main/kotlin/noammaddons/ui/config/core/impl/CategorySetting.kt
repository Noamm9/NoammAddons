package noammaddons.ui.config.core.impl

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.lerpColor
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.StencilUtils
import java.awt.Color
import kotlin.reflect.KProperty

class CategorySetting(
    name: String,
    val components: List<Component<*>>,
    defaultExpanded: Boolean = true,
    override val defaultValue: Unit = Unit
): Component<Unit>(name) {

    private var isExpanded = defaultExpanded
    private val headerHeight = 22.0

    private var expandAnimProgress = if (isExpanded) 1.0 else 0.0
    private var hoverAnimProgress = 0.0
    private var isHovered = false

    init {
        updateHeight()
    }

    private fun updateHeight() {
        val contentHeight = components.sumOf { it.height }
        height = headerHeight + (contentHeight * expandAnimProgress)
    }

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentlyHovered = isMouseOverHeader(x, y, mouseX, mouseY)
        if (currentlyHovered != isHovered) {
            isHovered = currentlyHovered
            animateHover(isHovered)
        }

        val headerBgColor = lerpColor(compBackgroundColor, hoverColor, hoverAnimProgress)
        drawSmoothRect(headerBgColor, x, y, width, headerHeight)
        textRenderer.drawText(name, x + 6, y + (headerHeight - textRenderer.fr.fontHeight) / 2 + 1)

        // --- Draw Arrow ---
        val arrowRotation = (expandAnimProgress * 90.0).toFloat()
        val arrowSize = 6.0
        val arrowX = x + width - arrowSize - 8
        val arrowY = y + (headerHeight / 2.0)
        // drawTriangle(Color.WHITE, arrowX, arrowY, arrowSize, arrowRotation)

        // --- Draw Children (if expanded/animating) ---
        if (expandAnimProgress > 0.0) {
            val contentHeight = components.sumOf { it.height }
            val animatedContentHeight = contentHeight * expandAnimProgress
            val contentY = y + headerHeight

            StencilUtils.beginStencilClip {
                drawRect(Color.WHITE, x, contentY, width, animatedContentHeight)
            }

            var childOffsetY = 0.0
            for (component in components) {
                component.width = this.width - 8
                val componentX = x + 4
                val componentY = contentY + childOffsetY

                if (componentY < contentY + animatedContentHeight) {
                    component.draw(componentX, componentY, mouseX, mouseY)
                }
                childOffsetY += component.height
            }
            StencilUtils.endStencilClip()
        }
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (isMouseOverHeader(x, y, mouseX, mouseY)) {
            isExpanded = ! isExpanded
            animateExpand()
        }
        else if (isExpanded) {
            var childOffsetY = 0.0
            for (component in components) {
                val componentY = y + headerHeight + childOffsetY
                if (mouseY >= componentY && mouseY < componentY + component.height) {
                    component.mouseClicked(x + 4, componentY, mouseX, mouseY, button)
                }
                childOffsetY += component.height
            }

            // @formatter:off
            scope.launch { repeat(250) { updateHeight(); delay(1) } }
        }
    }

    override fun mouseRelease(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (! isExpanded) return
        var childOffsetY = 0.0
        for (component in components) {
            val componentY = y + headerHeight + childOffsetY
            if (mouseY >= componentY && mouseY < componentY + component.height) {
                component.mouseRelease(x + 4, componentY, mouseX, mouseY, button)
            }
            childOffsetY += component.height
        }
    }

    override fun mouseDragged(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (! isExpanded) return
        var childOffsetY = 0.0
        for (component in components) {
            val componentY = y + headerHeight + childOffsetY
            if (mouseY >= componentY && mouseY < componentY + component.height) {
                component.mouseDragged(x + 4, componentY, mouseX, mouseY, button)
            }
            childOffsetY += component.height
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (! isExpanded) return false
        return components.any { it.keyTyped(typedChar, keyCode) }
    }


    private fun isMouseOverHeader(x: Double, y: Double, mouseX: Double, mouseY: Double) =
        mouseX in x .. (x + width) && mouseY in y .. (y + headerHeight)

    private fun animateExpand() = scope.launch {
        val start = expandAnimProgress
        val end = if (isExpanded) 1.0 else 0.0
        val duration = 300L

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val t = (System.currentTimeMillis() - startTime).toDouble() / duration
            expandAnimProgress = lerp(start, end, easeOutQuad(t))
            updateHeight()
            delay(7)
        }
        expandAnimProgress = end
        updateHeight()
    }

    private fun animateHover(hovering: Boolean) = scope.launch {
        val start = hoverAnimProgress
        val end = if (hovering) 1.0 else 0.0
        val duration = 150L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val t = (System.currentTimeMillis() - startTime).toDouble() / duration
            hoverAnimProgress = lerp(start, end, easeOutQuad(t))
            delay(7)
        }
        hoverAnimProgress = end
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = Unit
}