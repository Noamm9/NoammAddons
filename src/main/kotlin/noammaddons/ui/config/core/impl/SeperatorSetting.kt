package noammaddons.ui.config.core.impl

import noammaddons.features.Feature
import noammaddons.utils.RenderUtils.drawCenteredText
import java.awt.Color
import kotlin.reflect.KProperty

class SeperatorSetting(label: String): Component<Unit>(label) {
    override val defaultValue = Unit
    override val height = 30.0

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        drawSmoothRect(Color.WHITE, x, y + 20, width, 20 / 10)
        drawCenteredText(name, x + width / 2, y + 6)
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) {
        throw Error("No wtf are u doing?")
    }
}