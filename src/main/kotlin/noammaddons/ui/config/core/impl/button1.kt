package noammaddons.ui.config.core.impl

import noammaddons.features.Feature
import noammaddons.noammaddons
import noammaddons.ui.config.ConfigGUI
import noammaddons.ui.config.ConfigGUI.componentsScroll
import noammaddons.ui.config.core.SubCategory
import noammaddons.utils.MathUtils.interpolateColor
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils
import kotlin.reflect.KProperty

open class button1(name: String, val cat: SubCategory): Component<() -> Unit>(name) {
    override val width get() = Companion.width
    override val defaultValue = {}

    private var borderAnimation = 0f
    private var hoverAnimation = 0f

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val hovered = mouseX in x .. (x + width) && mouseY in y .. (y + 20)
        hoverAnimation = (hoverAnimation + (if (! hovered) 1f else 0f - hoverAnimation) * 0.1f).coerceIn(0f, 1f)
        borderAnimation = (borderAnimation + (if (cat.feature.enabled) 1f else 0f - borderAnimation) * 0.1f).coerceIn(0f, 1f)

        val color = interpolateColor(hoverColor, compBackgroundColor, hoverAnimation)
        val borderColor = interpolateColor(if (hovered) compBackgroundColor else hoverColor, accentColor, borderAnimation)
        drawSmoothRect(borderColor, x - 1, y - 1, width + 2, height + 2)
        drawSmoothRect(color, x, y, width, height)
        drawText(name, x + 6, y + 6)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX !in x .. x + width || mouseY !in y .. y + height) return
        SoundUtils.click()

        when (button) {
            0 -> cat.feature.toggle()

            1 -> {
                ConfigGUI.selectedSubCategory = cat.takeIf {
                    ConfigGUI.selectedSubCategory != cat && cat.components.isNotEmpty()
                }
                componentsScroll = 0f
            }
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>): () -> Unit {
        throw Error("Wtf are you doing? why?")
    }

    companion object {
        val width get() = noammaddons.mc.getWidth().times(0.20)
    }
}