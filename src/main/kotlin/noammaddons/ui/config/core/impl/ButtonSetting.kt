package noammaddons.ui.config.core.impl

import noammaddons.features.Feature
import noammaddons.NoammAddons.Companion.textRenderer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.reflect.KProperty

class ButtonSetting(
    name: String,
    override val defaultValue: Runnable
): Component<Runnable>(name) {
    private var isClickAnimating: Boolean = false
    private var clickAnimationStartMillis: Long = 0L
    private val clickAnimationDurationMillis: Long = 150L
    private val clickMinScale: Float = 0.95f
    private val flashMaxAlphaProportion: Float = 0.7f

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val now = System.currentTimeMillis()
        var currentScale = 1.0f
        var flashAlphaModifier = 0.0f

        if (isClickAnimating) {
            val elapsedMillis = now - clickAnimationStartMillis
            if (elapsedMillis >= clickAnimationDurationMillis) {
                isClickAnimating = false
            }
            else {
                val progress = elapsedMillis.toDouble() / clickAnimationDurationMillis
                val scaleEffect = sin(progress * PI).toFloat()
                currentScale = 1.0f - (1.0f - clickMinScale) * scaleEffect
                val flashProgress = (progress * 1.5).coerceAtMost(1.0)
                flashAlphaModifier = (1.0f - flashProgress.toFloat()) * flashMaxAlphaProportion
            }
        }

        val scaledWidth = width * currentScale
        val scaledHeight = height * currentScale
        val drawX = x + (width - scaledWidth) / 2.0
        val drawY = y + (height - scaledHeight) / 2.0

        val hovered = mouseX in x .. (x + width) && mouseY in y .. (y + height)
        val baseColor = if (hovered && ! isClickAnimating) hoverColor else compBackgroundColor

        drawSmoothRect(baseColor, drawX, drawY, scaledWidth, scaledHeight)

        if (isClickAnimating && flashAlphaModifier > 0) {
            drawSmoothRect(baseColor, drawX, drawY, scaledWidth, scaledHeight)
        }

        textRenderer.drawCenteredText(name, x + width / 2.0, drawY + 0.3 * scaledHeight)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX in x .. (x + width) && mouseY in y .. (y + height)) {
            isClickAnimating = true
            clickAnimationStartMillis = System.currentTimeMillis()
            defaultValue.run()
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = defaultValue

    fun invoke() = defaultValue.run()
}