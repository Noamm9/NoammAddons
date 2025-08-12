package noammaddons.ui.config.core.impl

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.lerpColor
import java.awt.Color
import kotlin.math.PI
import kotlin.math.sin
import kotlin.reflect.KProperty

class ButtonSetting(
    name: String,
    override val defaultValue: Runnable
): Component<Runnable>(name) {
    private var isClickAnimating: Boolean = false
    private var clickAnimationStartMillis: Long = 0L
    private val clickAnimationDurationMillis: Long = 200L
    private val clickMinScale: Float = 0.95f
    private val flashMaxAlpha: Int = (255 * 0.5f).toInt()

    private var hoverAnimProgress = 0.0
    private var isHovered = false

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentlyHovered = mouseX in x .. (x + width) && mouseY in y .. (y + height) && ! isClickAnimating
        if (currentlyHovered != isHovered) {
            isHovered = currentlyHovered
            animateHover(isHovered)
        }

        val now = System.currentTimeMillis()
        var currentScale = 1.0f
        var flashAlpha = 0

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
                flashAlpha = ((1.0f - flashProgress.toFloat()) * flashMaxAlpha).toInt()
            }
        }

        val scaledWidth = width * currentScale
        val scaledHeight = height * currentScale
        val drawX = x + (width - scaledWidth) / 2.0
        val drawY = y + (height - scaledHeight) / 2.0

        val baseColor = lerpColor(compBackgroundColor, hoverColor, hoverAnimProgress)
        drawSmoothRect(baseColor, drawX, drawY, scaledWidth, scaledHeight)

        if (isClickAnimating && flashAlpha > 0) {
            val flashColor = Color(255, 255, 255, flashAlpha)
            drawSmoothRect(flashColor, drawX, drawY, scaledWidth, scaledHeight)
        }

        textRenderer.drawCenteredText(name, x + width / 2.0, y + (height - textRenderer.fr.fontHeight) / 2.0 + 1)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX in x .. (x + width) && mouseY in y .. (y + height) && ! isClickAnimating && button == 0) {
            isClickAnimating = true
            clickAnimationStartMillis = System.currentTimeMillis()
            defaultValue.run()
        }
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

    override fun getValue(thisRef: Feature, property: KProperty<*>) = defaultValue

    fun invoke() = defaultValue.run()
}