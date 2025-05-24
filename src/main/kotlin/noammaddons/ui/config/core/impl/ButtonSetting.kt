package noammaddons.ui.config.core.impl

import noammaddons.features.Feature
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.SoundUtils
import kotlin.reflect.KProperty

class ButtonSetting(name: String, override val defaultValue: Runnable): Component<Runnable>(name) {
    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val hovered = mouseX in x .. (x + width) && mouseY in y .. (y + 20)
        val color = if (hovered) hoverColor else compBackgroundColor
        drawSmoothRect(color, x, y, width, height)
        drawCenteredText(name, x + width / 2, y + 6)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX in x .. x + width && mouseY in y .. y + height) {
            defaultValue.run()
            SoundUtils.click()
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>): Runnable {
        return value
    }

    fun invoke() = defaultValue.run()
}