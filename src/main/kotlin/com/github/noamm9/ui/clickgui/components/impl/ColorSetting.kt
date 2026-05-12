package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.Savable
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.render.Render2D
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.*

class ColorSetting(name: String, defaultValue: Color, val withAlpha: Boolean = true): Setting<Color>(name, defaultValue), Savable {
    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    private var h: Float = 0f
    private var s: Float = 0f
    private var b: Float = 0f
    private var a: Float = 1f

    private var draggingSV = false
    private var draggingHue = false
    private var draggingAlpha = false

    private val validHexChars = "0123456789ABCDEFabcdef"
    private var hexFocused = false
    private var hexText = ""

    init {
        val hsb = Color.RGBtoHSB(value.red, value.green, value.blue, null)
        h = hsb[0]; s = hsb[1]; b = hsb[2]
        a = value.alpha / 255f
        updateHexText()
    }

    private fun updateColorFromHSB() {
        val rgb = Color.HSBtoRGB(h, s, b)
        super.value = Color(rgb).withAlpha((a * 255).toInt())
        updateHexText()
    }

    private fun updateHexText() {
        hexText = if (withAlpha) String.format(Locale.US, "%02x%02x%02x%02x", value.alpha, value.red, value.green, value.blue)
        else String.format(Locale.US, "%02x%02x%02x", value.red, value.green, value.blue)
        hexText = hexText.uppercase()
    }

    override val height get() = 20 + (openAnim.value * 115).toInt()

    override fun draw(ctx: GuiGraphics, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x.toFloat(), y.toFloat(), width.toFloat(), 20f)
        Style.drawHoverBar(ctx, x.toFloat(), y.toFloat(), 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val previewX = x + width - 18f
        if (withAlpha) drawCheckerboard(ctx, previewX, y + 6f, 8f, 8f, 2)
        Render2D.drawRect(ctx, previewX, y + 6f, 8f, 8f, value)

        ctx.enableScissor(x, y, x + width, y + height)

        if (expanded) {
            val pickerY = y + 25f
            val pickerSize = 80f
            handleInputs(mouseX, mouseY, pickerY, pickerSize)

            var currentX = x + 10f
            if (withAlpha) {
                drawVerticalAlphaBar(ctx, currentX, pickerY, 10f, pickerSize)
                currentX += 15f
            }
            drawVerticalHueBar(ctx, currentX, pickerY, 10f, pickerSize)
            currentX += 15f
            drawSVBox(ctx, currentX, pickerY, (x + width - 10f) - currentX, pickerSize)

            val hexY = pickerY + pickerSize + 5f
            val hexW = width - 20f
            Render2D.drawRect(ctx, x + 10f, hexY, hexW, 12f, Color(10, 10, 10, 200))
            if (hexFocused) Render2D.drawRect(ctx, x + 10f, hexY + 11f, hexW, 1f, Style.accentColor)

            val cursor = if (hexFocused && (System.currentTimeMillis() / 500) % 2 == 0L) "|" else ""
            Render2D.drawString(ctx, "Hex: §7#$hexText$cursor", x + 14f, hexY + 2f)
        }

        ctx.disableScissor()
    }

    private fun handleInputs(mx: Int, my: Int, py: Float, ps: Float) {
        if (hexFocused) return
        val lmb = GLFW.glfwGetMouseButton(NoammAddons.mc.window.handle(), 0) == 1
        if (! lmb) {
            draggingSV = false
            draggingHue = false
            draggingAlpha = false
            return
        }

        var currentX = x + 10f
        val aX = currentX
        if (withAlpha) currentX += 15f
        val hX = currentX
        currentX += 15f
        val svX = currentX
        val svW = (x + width - 10f) - svX

        if (! draggingSV && ! draggingHue && ! draggingAlpha) {
            draggingAlpha = withAlpha && mx >= aX && mx <= aX + 10 && my >= py && my <= py + ps
            draggingHue = mx >= hX && mx <= hX + 10 && my >= py && my <= py + ps
            draggingSV = mx >= svX && mx <= svX + svW && my >= py && my <= py + ps
        }

        if (draggingAlpha) a = (1f - (my - py) / ps).coerceIn(0f, 1f)
        if (draggingHue) h = ((my - py) / ps).coerceIn(0f, 1f)
        if (draggingSV) {
            s = ((mx - svX) / svW).coerceIn(0f, 1f)
            b = (1f - (my - py) / ps).coerceIn(0f, 1f)
        }

        if (draggingSV || draggingHue || draggingAlpha) {
            updateColorFromHSB()
        }
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (expanded && hexFocused) {
            val codePoint = codePoint.lowercase()
            if (validHexChars.contains(codePoint) && hexText.length < (if (withAlpha) 8 else 6)) {
                hexText += codePoint.uppercase()
                tryUpdateFromHex()
            }
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (expanded && hexFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && hexText.isNotEmpty()) {
                hexText = hexText.dropLast(1)
                tryUpdateFromHex()
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) hexFocused = false
            return true
        }
        return false
    }

    private fun tryUpdateFromHex() {
        val req = if (withAlpha) 8 else 6
        if (hexText.length == req) catch {
            val longVal = hexText.toLong(16)
            val c = if (withAlpha) Color((longVal and 0xFFFFFFFFL).toInt(), true)
            else Color((longVal and 0xFFFFFFL).toInt())

            val hsb = Color.RGBtoHSB(c.red, c.green, c.blue, null)
            h = hsb[0]
            s = hsb[1]
            b = hsb[2]
            a = c.alpha / 255f

            super.value = c
        }
    }

    private fun drawSVBox(ctx: GuiGraphics, sx: Float, sy: Float, sw: Float, sh: Float) {
        Render2D.drawRect(ctx, sx, sy, sw, sh, Color.getHSBColor(h, 1f, 1f))
        Render2D.drawHorizontalGradient(ctx, sx, sy, sw, sh, Color.WHITE, Color(255, 255, 255, 0))
        Render2D.drawVerticalGradient(ctx, sx, sy, sw, sh, Color(0, 0, 0, 0), Color.BLACK)

        val ix = sx + (s * sw)
        val iy = sy + ((1f - b) * sh)
        Render2D.drawRect(ctx, ix - 1.5f, iy - 1.5f, 3f, 3f, Color.WHITE)
        Render2D.drawRect(ctx, ix - 0.5f, iy - 0.5f, 1f, 1f, Color.BLACK)
    }

    private fun drawVerticalHueBar(ctx: GuiGraphics, hx: Float, hy: Float, hw: Float, hh: Float) {
        val segments = 12
        val step = hh / segments.toFloat()

        for (i in 0 until segments) {
            val c1 = Color.getHSBColor(i / segments.toFloat(), 1f, 1f)
            val c2 = Color.getHSBColor((i + 1) / segments.toFloat(), 1f, 1f)
            val yStart = hy + (i * step)
            val yEnd = hy + ((i + 1) * step)
            Render2D.drawVerticalGradient(ctx, hx, yStart, hw, (yEnd - yStart) + 0.5f, c1, c2)
        }

        Render2D.drawRect(ctx, hx - 1f, hy + (h * hh) - 0.5f, hw + 2f, 1f, Color.WHITE)
    }

    private fun drawVerticalAlphaBar(ctx: GuiGraphics, ax: Float, ay: Float, aw: Float, ah: Float) {
        drawCheckerboard(ctx, ax, ay, aw, ah, 2)
        val base = Color(Color.HSBtoRGB(h, s, b))
        val cTop = base.withAlpha(255)
        val cBot = base.withAlpha(0)

        Render2D.drawVerticalGradient(ctx, ax, ay, aw, ah, cTop, cBot)
        Render2D.drawRect(ctx, ax - 1f, ay + ((1f - a) * ah) - 0.5f, aw + 2f, 1f, Color.WHITE)
    }

    private fun drawCheckerboard(ctx: GuiGraphics, x: Float, y: Float, w: Float, h: Float, size: Int) {
        for (i in 0 until (w / size).toInt()) {
            for (j in 0 until (h / size).toInt()) {
                val color = if ((i + j) % 2 == 0) Color(50, 50, 50, 200) else Color(30, 30, 30, 200)
                Render2D.drawRect(ctx, x + i * size, y + j * size, size.toFloat(), size.toFloat(), color)
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            if (button == 0) {
                expanded = ! expanded
                return true
            }
        }
        if (expanded) {
            val hexY = y + 25f + 80f + 5f
            hexFocused = mouseX >= x + 10 && mouseX <= x + width - 10 && mouseY >= hexY && mouseY <= hexY + 12
            if (hexFocused) return true
        }
        return false
    }

    override fun write(): JsonElement = JsonPrimitive(value.rgb)
    override fun read(element: JsonElement?) {
        element?.asInt?.let {
            super.value = Color(it, true)
            val hsb = Color.RGBtoHSB(value.red, value.green, value.blue, null)
            h = hsb[0]; s = hsb[1]; b = hsb[2]; a = value.alpha / 255f
            updateHexText()
        }
    }
}