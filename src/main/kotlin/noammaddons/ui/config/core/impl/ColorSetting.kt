package noammaddons.ui.config.core.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import gg.essential.elementa.utils.withAlpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.ui.config.core.save.Savable
import noammaddons.utils.MathUtils.lerp
import noammaddons.utils.MathUtils.lerpColor
import noammaddons.utils.MouseUtils.isMouseOver
import noammaddons.utils.RenderHelper.colorFromHSB
import noammaddons.utils.RenderUtils.drawCheckerboard
import noammaddons.utils.RenderUtils.drawGradientRect
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawRectBorder
import noammaddons.utils.StencilUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.reflect.KProperty


class ColorSetting(
    name: String, override var defaultValue: Color, val withAlpha: Boolean = true
): Component<Color>(name), Savable {
    private var isPickerOpen = false
    override var value: Color = defaultValue

    private var pickerX = 0.0
    private var pickerY = 0.0
    private val pickerWidth = 140.0
    private val pickerHeight = 110.0

    private var currentHue = 0f
    private var currentSaturation = 1f
    private var currentBrightness = 1f
    private var currentAlpha = 1f

    private var draggingSatBri = false
    private var draggingHue = false
    private var draggingAlpha = false

    private val borderColor = Color.white

    private var expanded = false
    private var expandAnimProgress = 0.0

    private var isHovered = false
    private var hoverAnimProgress = 0.0

    private val collapsedHeight = 22.0
    private val expandedHeight = collapsedHeight + pickerHeight + 3

    override var height = collapsedHeight

    override fun draw(x: Double, y: Double, mouseX: Double, mouseY: Double) {
        val currentlyHovered = mouseX in x .. (x + width) && mouseY in y .. (y + collapsedHeight)
        if (currentlyHovered != isHovered) {
            isHovered = currentlyHovered
            animateHover(isHovered)
        }

        val headerBgColor = lerpColor(compBackgroundColor, hoverColor, hoverAnimProgress)
        drawSmoothRect(headerBgColor, x, y, width, collapsedHeight)

        if (height > collapsedHeight) drawSmoothRect(compBackgroundColor, x, y + collapsedHeight, width, height - collapsedHeight)

        textRenderer.drawText(name, x + 6, y + 7)

        val previewX = x + width - 22
        val previewY = y + 3
        drawCheckerboard(previewX, previewY, 16, 16, 2)
        drawRect(value, previewX, previewY, 16, 16)

        StencilUtils.beginStencilClip {
            drawRect(Color(0), x, y, width, height)
        }

        if (isPickerOpen) {
            pickerX = x + 6
            pickerY = y + 27
            drawPicker(pickerX, pickerY)
        }

        StencilUtils.endStencilClip()
    }

    override fun mouseClicked(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (! button.equalsOneOf(0, 1)) return
        if (isMouseOver(mouseX, mouseY, x, y, width, collapsedHeight)) {
            expanded = ! expanded
            animateExpand()
            if (expanded) togglePicker()
            else setTimeout(200, ::togglePicker)
            return
        }

        if (! expanded || button != 0 || ! isPickerOpen) return
        if (isMouseOver(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
            onPickerInteraction(mouseX, mouseY, isClick = true)
        }
    }

    override fun mouseDragged(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        if (button == 0 && isPickerOpen) onPickerInteraction(mouseX, mouseY, isClick = false)
    }

    override fun mouseRelease(x: Double, y: Double, mouseX: Double, mouseY: Double, button: Int) {
        draggingSatBri = false
        draggingHue = false
        draggingAlpha = false
    }

    private fun togglePicker() {
        if (isPickerOpen) isPickerOpen = false
        else {
            isPickerOpen = true
            val hsb = Color.RGBtoHSB(value.red, value.green, value.blue, null)
            currentHue = hsb[0]
            currentSaturation = hsb[1]
            currentBrightness = hsb[2]
            currentAlpha = value.alpha / 255f
        }
    }

    private fun drawPicker(x: Double, y: Double) {
        val satBriX = x + 5
        val satBriY = y + 5
        val satBriW = pickerWidth - 50
        val satBriH = pickerHeight - 20

        val hueX = satBriX + satBriW + if (withAlpha) 5 else 8
        val hueW = 15.0

        drawRect(compBackgroundColor, x, y, if (withAlpha) pickerWidth else pickerWidth - 15.0, pickerHeight - 10)
        drawRectBorder(borderColor, x, y, if (withAlpha) pickerWidth else pickerWidth - 15.0, pickerHeight - 10)

        drawSvBox(satBriX, satBriY)
        val satBriIndicatorX = satBriX + satBriW * currentSaturation
        val satBriIndicatorY = satBriY + satBriH * (1 - currentBrightness)
        drawRect(Color.WHITE, satBriIndicatorX - 2, satBriIndicatorY - 2, 4.0, 4.0)
        drawRect(Color.BLACK, satBriIndicatorX - 1, satBriIndicatorY - 1, 2.0, 2.0)

        drawHueSlider(hueX, satBriY)
        val hueIndicatorY = satBriY + satBriH * currentHue
        drawRect(Color.WHITE, hueX - 2, hueIndicatorY - 1, hueW + 4, 2.0)

        if (withAlpha) {
            val alphaX = hueX + hueW + 5
            val alphaY = y + 5
            val alphaW = 15.0

            drawAlphaSlider(alphaX, alphaY)
            val alphaIndicatorY = alphaY + satBriH * (1 - currentAlpha)
            drawRect(Color.WHITE, alphaX - 2, alphaIndicatorY - 1, alphaW + 4, 2.0)
        }
    }

    private fun onPickerInteraction(mouseX: Double, mouseY: Double, isClick: Boolean) {
        val satBriX = pickerX + 5
        val satBriY = pickerY + 5
        val satBriW = pickerWidth - 50
        val satBriH = pickerHeight - 20
        val hueX = satBriX + satBriW + 5
        val alphaX = hueX + 15 + 5

        if (isClick) {
            draggingSatBri = isMouseOver(mouseX, mouseY, satBriX, satBriY, satBriW, satBriH)
            draggingHue = isMouseOver(mouseX, mouseY, hueX, satBriY, 15.0, satBriH)
            draggingAlpha = isMouseOver(mouseX, mouseY, alphaX, satBriY, 15.0, satBriH)
        }

        if (draggingSatBri) {
            currentSaturation = ((mouseX - satBriX) / satBriW).toFloat().coerceIn(0f, 1f)
            currentBrightness = (1f - (mouseY - satBriY) / satBriH).toFloat().coerceIn(0f, 1f)
        }

        if (draggingHue) currentHue = ((mouseY - satBriY) / satBriH).toFloat().coerceIn(0f, 1f)
        if (draggingAlpha) currentAlpha = (1f - (mouseY - satBriY) / satBriH).toFloat().coerceIn(0f, 1f)
        if (draggingSatBri || draggingHue || draggingAlpha) recalculateColor()
    }

    private fun recalculateColor() {
        this.value = colorFromHSB(currentHue, currentSaturation, currentBrightness)
            .withAlpha((currentAlpha * 255).roundToInt().coerceIn(0, 255))
    }

    private fun drawSvBox(x: Double, y: Double) {
        val satBriW = 90.0
        val satBriH = 90.0

        val pureHueColor = colorFromHSB(currentHue, 1.0f, 1.0f)
        drawRect(pureHueColor, x, y, satBriW, satBriH)

        val white = Color.WHITE
        val transparentWhite = white.withAlpha(0)
        drawGradientRect(x, y, satBriW, satBriH, white, transparentWhite, white, transparentWhite)

        val black = Color.BLACK
        val transparentBlack = black.withAlpha(0)
        drawGradientRect(x, y, satBriW, satBriH, transparentBlack, transparentBlack, black, black)

        drawRectBorder(borderColor, x - 1, y - 1, satBriW + 2, satBriH + 2)
    }

    private fun drawHueSlider(x: Double, y: Double) {
        val segments = 6
        val segmentHeight = 90.0 / segments

        for (i in 0 until segments) {
            val hue1 = i.toFloat() / segments
            val hue2 = (i + 1).toFloat() / segments
            val color1 = colorFromHSB(hue1, 1.0f, 1.0f)
            val color2 = colorFromHSB(hue2, 1.0f, 1.0f)
            drawGradientRect(x, y + i * segmentHeight, 15, segmentHeight, color1, color2)
        }

        drawRectBorder(borderColor, x, y, 15, 90)
    }

    private fun drawAlphaSlider(x: Double, y: Double) {
        drawCheckerboard(x, y, 15, 90, 4.0)

        val baseColor = colorFromHSB(currentHue, currentSaturation, currentBrightness)
        val startColor = Color(baseColor.red, baseColor.green, baseColor.blue, 255)
        val endColor = Color(baseColor.red, baseColor.green, baseColor.blue, 0)

        drawGradientRect(x, y, 15, 90, startColor, endColor)
        drawRectBorder(borderColor, x, y, 15, 90)
    }

    private fun animateHover(hovering: Boolean) = scope.launch {
        val start = hoverAnimProgress
        val end = if (hovering) 1.0 else 0.0
        val duration = 0.15
        val startTime = System.nanoTime()
        while (true) {
            val elapsed = (System.nanoTime() - startTime) / 1e9
            if (elapsed >= duration) break
            val t = elapsed / duration
            hoverAnimProgress = lerp(start, end, easeOutQuad(t))
            delay(7)
        }
        hoverAnimProgress = end
    }

    private fun animateExpand() {
        scope.launch {
            val start = expandAnimProgress
            val end = if (expanded) 1.0 else 0.0
            val duration = 250L

            val startTime = System.currentTimeMillis()
            var elapsedTime: Long

            while (System.currentTimeMillis() - startTime < duration) {
                elapsedTime = System.currentTimeMillis() - startTime
                val t = elapsedTime.toDouble() / duration
                expandAnimProgress = lerp(start, end, easeOutQuad(t))
                delay(7)
                height = collapsedHeight + expandAnimProgress * (expandedHeight - collapsedHeight)
            }
            expandAnimProgress = end
            height = collapsedHeight + expandAnimProgress * (expandedHeight - collapsedHeight)
        }
    }

    override fun getValue(thisRef: Feature, property: KProperty<*>) = value

    override fun write(): JsonElement {
        return JsonPrimitive(value.rgb)
    }

    override fun read(element: JsonElement?) {
        element?.let { element1 ->
            element1.asInt.let {
                value = Color(it, withAlpha)
            }
        }
    }
}