package noammaddons.ui.config.core.impl

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.DungeonMap
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.ui.config.ConfigGUI
import noammaddons.ui.config.ConfigGUI.settingsScroll
import noammaddons.ui.config.core.FeatureElement
import noammaddons.ui.config.core.save.Config
import noammaddons.utils.MathUtils.lerpColor
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.RenderUtils.drawCircle
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import kotlin.reflect.KProperty


open class FeatureToggle(name: String, val cat: FeatureElement): Component<() -> Unit>(name) {
    override val defaultValue = {}

    private var animProgress = 0f
    private var hoverAnimation = 0f

    private val switchTrackWidth = 25.8
    private val switchTrackHeight = 12.0
    private val switchKnobRadius = (switchTrackHeight / 2.0) - 1.5
    private val switchPaddingRight = 10.0

    init {
        Config.initialised.onSetValue {
            if (! it) return@onSetValue
            animProgress = if (cat.feature.enabled) 1f else 0f
        }
    }

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val hovered = mouseX in x .. (x + width) && mouseY in y .. (y + 20)
        hoverAnimation = (hoverAnimation + (if (! hovered) 1f else 0f - hoverAnimation) * 0.1f).coerceIn(0f, 1f)
        val color = lerpColor(hoverColor, compBackgroundColor, hoverAnimation)

        drawSmoothRect(color, x, y, width, height)
        textRenderer.drawText(name, x + 6, y + 6)

        drawSwitch(x, y)
        drawSettingsIcon(x, y)
    }

    private fun drawSwitch(x: Double, y: Double) {
        if (cat.feature.equalsOneOf(DungeonMap)) return // Hotfix

        val trackX = x + width - switchPaddingRight - switchTrackWidth
        val trackY = y + (this.height - switchTrackHeight) / 2.0

        val currentTrackColor = lerpColor(Color(23, 23, 23), accentColor, animProgress)
        drawSmoothRect(currentTrackColor, trackX, trackY, switchTrackWidth, switchTrackHeight)

        val knobDiameter = switchKnobRadius * 2.0
        val travelDistance = switchTrackWidth - knobDiameter - (switchKnobRadius * 0.5)

        val knobCenterX = trackX + switchKnobRadius + (switchKnobRadius * 0.25) + (travelDistance * animProgress)
        val knobCenterY = trackY + switchTrackHeight / 2.0

        val knobDrawX = knobCenterX - switchKnobRadius
        val knobDrawY = knobCenterY - switchKnobRadius

        drawSmoothRect(Color.WHITE, knobDrawX, knobDrawY, knobDiameter, knobDiameter)
    }

    private fun drawSettingsIcon(x: Double, y: Double) {
        if (cat.feature.configSettings.isEmpty()) return

        val iconCenterX = (x + width - switchPaddingRight * 2f - switchTrackWidth).toFloat()
        val iconCenterY = (y + height / 2.0f).toFloat()
        val dotRadius = 1.5f
        val spacing = 5.0f

        val yTop = iconCenterY - spacing
        val yBottom = iconCenterY + spacing
        val dotFillColor = Color.white

        drawCircle(dotFillColor, iconCenterX, yTop, dotRadius)
        drawCircle(dotFillColor, iconCenterX, iconCenterY, dotRadius)
        drawCircle(dotFillColor, iconCenterX, yBottom, dotRadius)
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (mouseX !in x .. x + width || mouseY !in y .. y + height && button != 0) return

        val trackX = x + width - switchPaddingRight - switchTrackWidth
        val trackY = y + (this.height - switchTrackHeight) / 2.0

        // HotFix for DungeonMap 
        if (mouseX >= trackX && mouseX <= (trackX + switchTrackWidth) && mouseY >= trackY && mouseY <= (trackY + switchTrackHeight) && ! cat.feature.equalsOneOf(DungeonMap)) {
            animate()
            cat.feature.toggle()
        }
        else if (cat.feature.configSettings.isNotEmpty()) {
            val iconCenterX = (x + width - switchPaddingRight * 2f - switchTrackWidth).toFloat()
            val iconCenterY = (y + height / 2.0f).toFloat()

            val spacing = 5.0f
            val dotRadius = 1.5f

            val yTop = iconCenterY - spacing
            val borderPadding = 1.0f

            val rectX = iconCenterX - dotRadius - borderPadding - 1
            val rectY = yTop - dotRadius - borderPadding
            val rectWidth = (dotRadius * 2) + (borderPadding * 2) + 3
            val rectHeight = (2 * spacing + 2 * dotRadius) + (borderPadding * 2)

            if (mouseX in rectX .. (rectX + rectWidth) && mouseY in rectY .. (rectY + rectHeight)) {
                ConfigGUI.openedFeatureSettings = cat
                settingsScroll = 0f
            }
        }
    }

    private fun animate() = scope.launch {
        val startProgress = animProgress
        val endProgress = if (cat.feature.enabled) 1f else 0f
        if (startProgress == endProgress) return@launch

        val durationSeconds = 0.20
        val startTimeNanos = System.nanoTime()

        while (true) {
            val elapsedNanos = System.nanoTime() - startTimeNanos
            val elapsedSeconds = elapsedNanos / 1_000_000_000.0
            val t = (elapsedSeconds / durationSeconds).coerceIn(0.0, 1.0)
            animProgress = lerp(startProgress, endProgress, easeOutQuad(t)).toFloat()
            if (t == 1.0) break
            delay(7)
        }
        animProgress = endProgress
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>): () -> Unit {
        throw Error("Wtf are you doing? why?")
    }
}