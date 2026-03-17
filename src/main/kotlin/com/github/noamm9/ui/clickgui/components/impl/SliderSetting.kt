package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.config.Savable
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.abs
import kotlin.math.round

open class SliderSetting<T: Number>(
    name: String,
    value: T,
    val min: T,
    val max: T,
    val step: T,
    val suffix: String = ""
): Setting<T>(name, value), Savable {
    private var dragging = false
    private var isTyping = false
    private var inputBuffer = ""

    private val hoverAnim = Animation(200)
    private val sliderAnim = Animation(250, getPercent(value))

    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        val target = getPercent(value)

        if (dragging && ! isTyping) {
            val pct = ((mouseX - (x + 8.0)) / (width - 16.0)).coerceIn(0.0, 1.0)
            val range = max.toDouble() - min.toDouble()
            val rawValue = min.toDouble() + (range * pct)
            value = snapToStep(rawValue)

            if (abs(sliderAnim.value - target) < 0.05f) sliderAnim.set(target)
        }

        hoverAnim.update(if (isHovered || dragging || isTyping) 1f else 0f)
        sliderAnim.update(target)

        Style.drawBackground(ctx, x, y, width, 20f)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 2f, hoverAnim.value)

        val valStr = if (isTyping) inputBuffer else stringfy(value) + suffix
        val textColor = if (isTyping) Style.accentColor else Color(180, 180, 180)
        Render2D.drawString(ctx, valStr, x + width - valStr.width() - 8f, y + 2f, textColor)

        Style.drawSlider(ctx, x + 8f, y + 14f, width - 16f, sliderAnim.value, hoverAnim.value, Style.accentColor)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            val valStrWidth = stringfy(value).width()
            val textX = x + width - valStrWidth - 8f

            if (mouseX >= textX && mouseY <= y + 12f) {
                isTyping = true
                inputBuffer = stringfy(value)
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
                GLFW.GLFW_KEY_ENTER -> {
                    val parsed = inputBuffer.toDoubleOrNull()
                    if (parsed != null) {
                        value = snapToStep(parsed)
                    }
                    isTyping = false
                }

                GLFW.GLFW_KEY_ESCAPE -> isTyping = false
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (inputBuffer.isNotEmpty()) inputBuffer = inputBuffer.dropLast(1)
                }
            }
            return true
        }
        return false
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (isTyping) {
            if (codePoint.isDigit() || codePoint == '.' || codePoint == '-') {
                inputBuffer += codePoint
            }
            return true
        }
        return false
    }
    

    private fun getPercent(valIn: T): Float {
        val current = valIn.toDouble()
        val minD = min.toDouble()
        val maxD = max.toDouble()
        if (maxD - minD == 0.0) return 0f
        return ((current - minD) / (maxD - minD)).toFloat()
    }

    private fun snapToStep(rawDouble: Double): T {
        val minD = min.toDouble()
        val maxD = max.toDouble()
        val stepD = step.toDouble()
        val clamped = rawDouble.coerceIn(minD, maxD)

        if (stepD <= 0) return clamped.convertToType()

        val steps = round((clamped - minD) / stepD)
        val steppedValue = (minD + (steps * stepD)).coerceIn(minD, maxD)

        return steppedValue.convertToType()
    }

    private fun Number.convertToType(): T {
        @Suppress("UNCHECKED_CAST")
        return when (min) {
            is Int -> toInt() as T
            is Long -> toLong() as T
            is Float -> toFloat() as T
            is Double -> toDouble() as T
            else -> this as T
        }
    }

    private fun stringfy(v: T): String {
        return when (v) {
            is Int, is Long -> v.toLong().toString()
            else -> {
                val dVal = v.toDouble()
                val stepD = step.toDouble()
                if (stepD % 1.0 == 0.0) dVal.toFixed(0)
                else dVal.toFixed(2)
            }
        }
    }

    override fun write(): JsonElement = JsonPrimitive(value)
    override fun read(element: JsonElement?) {
        element?.asNumber?.let { value = snapToStep(it.toDouble()) }
    }
}