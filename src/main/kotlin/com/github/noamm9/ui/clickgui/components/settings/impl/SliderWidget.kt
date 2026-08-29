package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.UKeyboard
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.abs

class SliderWidget<T: Number>(config: SliderSetting<T>): Widget<T>(config) {
    private inline val cfg get() = config as SliderSetting<T>

    private var dragging = false
    private var isTyping = false
    private var inputBuffer = ""

    private val hoverAnim = Animation(200)
    private val sliderAnim = Animation(250, cfg.getPercent(config.value))

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        val target = cfg.getPercent(value)

        if (dragging && ! isTyping) {
            val pct = ((mouseX - (x + 8.0)) / (width - 16.0)).coerceIn(0.0, 1.0)
            val range = cfg.max.toDouble() - cfg.min.toDouble()
            val rawValue = cfg.min.toDouble() + (range * pct)
            value = cfg.snapToStep(rawValue)

            if (abs(sliderAnim.value - target) < 0.05f) sliderAnim.set(target)
        }

        hoverAnim.update(if (isHovered || dragging || isTyping) 1f else 0f)
        sliderAnim.update(target)

        Style.drawBackground(ctx, x, y, width, 20f)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 2f, hoverAnim.value)

        val valStr = if (isTyping) inputBuffer else cfg.stringfy(value) + cfg.suffix
        val textColor = if (isTyping) Style.accentColor else Color(180, 180, 180)
        ctx.drawString(valStr, x + width - valStr.width() - 8f, y + 2f, textColor)

        Style.drawSlider(ctx, x + 8f, y + 14f, width - 16f, sliderAnim.value, hoverAnim.value, Style.accentColor)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            val valStrWidth = cfg.stringfy(value).width()
            val textX = x + width - valStrWidth - 8f

            if (mouseX >= textX && mouseY <= y + 12f) {
                isTyping = true
                inputBuffer = cfg.stringfy(value)
                dragging = false
            }
            else {
                isTyping = false
                dragging = true
            }
            return true
        }
        isTyping = false
        return false
    }

    override fun mouseReleased(button: Int) {
        dragging = false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (isTyping) {
            when (keyCode) {
                UKeyboard.KEY_ENTER -> {
                    val parsed = inputBuffer.toDoubleOrNull()
                    if (parsed != null) {
                        value = cfg.snapToStep(parsed)
                    }
                    isTyping = false
                }

                UKeyboard.KEY_ESCAPE -> isTyping = false
                UKeyboard.KEY_BACKSPACE -> {
                    if (inputBuffer.isNotEmpty()) inputBuffer = inputBuffer.dropLast(1)
                }
            }
            return true
        }
        return false
    }

    override fun charTyped(codePoint: Char): Boolean {
        if (isTyping) {
            if (codePoint.isDigit() || codePoint == '.' || codePoint == '-') {
                inputBuffer += codePoint
            }
            return true
        }
        return false
    }
}