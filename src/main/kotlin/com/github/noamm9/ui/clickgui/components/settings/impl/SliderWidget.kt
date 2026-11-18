package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.*
import java.awt.Color
import kotlin.math.abs

class SliderWidget<T: Number>(config: SliderSetting<T>): Widget<T>(config) {
    private inline val cfg get() = config as SliderSetting<T>

    private var dragging = false
    private var inputBuffer = ""
    private var inputHandler: TextInputHandler? = null
    private val isTyping get() = inputHandler?.listening == true

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
        if (isTyping) inputHandler?.apply {
            width = (valStr.width() + 8f).coerceAtMost(this@SliderWidget.width - 16f)
            x = this@SliderWidget.x + this@SliderWidget.width - width - 4f
            y = this@SliderWidget.y.toFloat()
            height = 14f
            draw(ctx, mouseX.toFloat(), mouseY.toFloat())
        }
        else ctx.drawString(valStr, x + width - valStr.width() - 8f, y + 2f, Color(180, 180, 180))

        Style.drawSlider(ctx, x + 8f, y + 14f, width - 16f, sliderAnim.value, Style.accentColor)
    }

    override fun clearFocus() {
        inputHandler?.resetState()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val event = MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, 0))
        if (isTyping && inputHandler?.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), event) == true) return true

        if (button == InputConstants.MOUSE_BUTTON_LEFT && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            val valStrWidth = cfg.stringfy(value).width()
            val textX = x + width - valStrWidth - 8f

            if (mouseX >= textX && mouseY <= y + 12f) {
                inputBuffer = cfg.stringfy(value)
                inputHandler = TextInputHandler({ inputBuffer }, { inputBuffer = it }).apply {
                    inputFilter = { text -> text.filter { it.isDigit() || it == '.' || it == '-' } }
                    listening = true
                }
                dragging = false
            }
            else {
                clearFocus()
                dragging = true
            }
            return true
        }
        clearFocus()
        return false
    }

    override fun mouseReleased(button: Int) {
        inputHandler?.mouseReleased()
        dragging = false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (! isTyping) return false
        if (keyCode == InputConstants.KEY_RETURN) {
            inputBuffer.toDoubleOrNull()?.let { value = cfg.snapToStep(it) }
        }
        inputHandler?.keyPressed(KeyEvent(keyCode, scanCode, modifiers))
        return true
    }

    override fun charTyped(codePoint: Char) = inputHandler?.keyTyped(CharacterEvent(codePoint.code)) ?: false
}