package noammaddons.ui.config.core.impl

import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.textRenderer
import java.awt.Color
import kotlin.reflect.KProperty

class SeperatorSetting(label: String): Component<Unit>(label) {
    override val defaultValue = Unit
    override var height = 20.0

    private val lineColor = Color.WHITE
    private val textColor = Color.WHITE
    private val lineThickness = 1.5
    private val paddingAroundText = 8.0

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentComponentWidth = width
        val textActualWidth = textRenderer.getStringWidth(name)
        val verticalCenterY = y + height / 2.0
        val lineTopY = verticalCenterY - (lineThickness / 2.0)
        val componentCenterX = x + currentComponentWidth / 2.0
        val textBlockWidth = textActualWidth + (paddingAroundText * 2)
        val availableWidthForLines = currentComponentWidth - textBlockWidth

        var eachLineWidth = 0.0
        if (availableWidthForLines > 0) eachLineWidth = availableWidthForLines / 2.0

        if (eachLineWidth > 0) drawSmoothRect(lineColor, x, lineTopY, eachLineWidth, lineThickness)
        textRenderer.drawCenteredText(name, componentCenterX, verticalCenterY - textRenderer.fr.fontHeight / 2 + 1, textColor)

        if (eachLineWidth > 0) {
            val rightLineStartX = x + eachLineWidth + textBlockWidth
            drawSmoothRect(lineColor, rightLineStartX, lineTopY, eachLineWidth, lineThickness)
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) {
        throw Error("wtf are u doing?")
    }
}