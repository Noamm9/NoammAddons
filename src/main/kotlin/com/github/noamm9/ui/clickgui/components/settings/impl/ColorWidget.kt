package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.render.Render2D.drawHorizontalGradient
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.drawVerticalGradient
import com.github.noamm9.utils.render.Render2D.scissor
import com.github.noamm9.utils.render.RenderHelper.width
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.*
import java.awt.Color
import java.util.*

class ColorWidget(config: ColorSetting): Widget<Color>(config) {
    private inline val cfg get() = config as ColorSetting

    override val height get() = 20 + (openAnim.value * 115).toInt()

    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    private var h: Float = 0f
    private var s: Float = 0f
    private var b: Float = 0f
    private var a: Float = 1f

    private var lastSynced = config.value

    private var draggingSV = false
    private var draggingHue = false
    private var draggingAlpha = false

    private val validHexChars = "0123456789ABCDEFabcdef"
    private var hexText = ""

    init {
        syncFromValue()
    }

    private val hexInput = TextInputHandler(
        textProvider = { hexText },
        textSetter = {
            hexText = it
            tryUpdateFromHex()
        }
    ).apply {
        maxLength = if (cfg.withAlpha) 8 else 6
        inputFilter = { text -> text.filter { it in validHexChars }.uppercase() }
    }

    private fun syncFromValue() {
        val hsb = Color.RGBtoHSB(value.red, value.green, value.blue, null)
        h = hsb[0]; s = hsb[1]; b = hsb[2]
        a = value.alpha / 255f
        updateHexText()
        lastSynced = value
    }

    private fun updateColorFromHSB() {
        val rgb = Color.HSBtoRGB(h, s, b)
        super.value = Color(rgb).withAlpha((a * 255).toInt())
        lastSynced = value
        updateHexText()
    }

    private fun updateHexText() {
        hexText = if (cfg.withAlpha) String.format(Locale.US, "%02x%02x%02x%02x", value.red, value.green, value.blue, value.alpha)
        else String.format(Locale.US, "%02x%02x%02x", value.red, value.green, value.blue)
        hexText = hexText.uppercase()
    }

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (value != lastSynced) syncFromValue()

        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x.toFloat(), y.toFloat(), width.toFloat(), 20f)
        Style.drawHoverBar(ctx, x.toFloat(), y.toFloat(), 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val previewX = x + width - 18f
        if (cfg.withAlpha) drawCheckerboard(ctx, previewX, y + 6f, 8f, 8f, 2)
        ctx.drawRect(previewX, y + 6f, 8f, 8f, value)

        ctx.scissor(x, y, width, height)

        if (expanded) {
            val pickerY = y + 25f
            val pickerSize = 80f
            handleInputs(mouseX, mouseY, pickerY, pickerSize)

            var currentX = x + 10f
            if (cfg.withAlpha) {
                drawVerticalAlphaBar(ctx, currentX, pickerY, 10f, pickerSize)
                currentX += 15f
            }
            drawVerticalHueBar(ctx, currentX, pickerY, 10f, pickerSize)
            currentX += 15f
            drawSVBox(ctx, currentX, pickerY, (x + width - 10f) - currentX, pickerSize)

            val hexY = pickerY + pickerSize + 5f
            val hexW = width - 20f
            ctx.drawRect(x + 10f, hexY, hexW, 18f, Color(10, 10, 10, 200))
            if (hexInput.listening) ctx.drawRect(x + 10f, hexY + 17f, hexW, 1f, Style.accentColor)

            val prefix = "Hex: #"
            ctx.drawString(prefix, x + 14f, hexY + 4f)
            hexInput.x = x + 10f + prefix.width()
            hexInput.y = hexY
            hexInput.width = hexW - prefix.width()
            hexInput.draw(ctx, mouseX.toFloat(), mouseY.toFloat())
        }

        ctx.disableScissor()
    }

    private fun handleInputs(mx: Int, my: Int, py: Float, ps: Float) {
        if (hexInput.listening) return
        if (! draggingSV && ! draggingHue && ! draggingAlpha) return

        var currentX = x + 10f
        if (cfg.withAlpha) currentX += 15f
        currentX += 15f
        val svX = currentX
        val svW = (x + width - 10f) - svX

        if (draggingAlpha) a = (1f - (my - py) / ps).coerceIn(0f, 1f)
        if (draggingHue) h = ((my - py) / ps).coerceIn(0f, 1f)
        if (draggingSV) {
            s = ((mx - svX) / svW).coerceIn(0f, 1f)
            b = (1f - (my - py) / ps).coerceIn(0f, 1f)
        }
        updateColorFromHSB()
    }

    override fun charTyped(codePoint: Char) = expanded && hexInput.keyTyped(CharacterEvent(codePoint.code))

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (! expanded || ! hexInput.listening) return false
        hexInput.keyPressed(KeyEvent(keyCode, scanCode, modifiers))
        return true
    }

    private fun tryUpdateFromHex() {
        val req = if (cfg.withAlpha) 8 else 6
        if (hexText.length == req) catch {
            val longVal = hexText.toLong(16)
            val c = if (cfg.withAlpha) {
                val r = ((longVal shr 24) and 0xFF).toInt()
                val g = ((longVal shr 16) and 0xFF).toInt()
                val b = ((longVal shr 8) and 0xFF).toInt()
                val a = (longVal and 0xFF).toInt()
                Color(r, g, b, a)
            }
            else Color((longVal and 0xFFFFFFL).toInt())

            val hsb = Color.RGBtoHSB(c.red, c.green, c.blue, null)
            h = hsb[0]
            s = hsb[1]
            b = hsb[2]
            a = c.alpha / 255f

            super.value = c
            lastSynced = c
        }
    }

    private fun drawSVBox(ctx: GuiGraphicsExtractor, sx: Float, sy: Float, sw: Float, sh: Float) {
        ctx.drawRect(sx, sy, sw, sh, Color.getHSBColor(h, 1f, 1f))
        ctx.drawHorizontalGradient(sx, sy, sw, sh, Color.WHITE, Color(255, 255, 255, 0))
        ctx.drawVerticalGradient(sx, sy, sw, sh, Color(0, 0, 0, 0), Color.BLACK)

        val ix = sx + (s * sw)
        val iy = sy + ((1f - b) * sh)
        ctx.drawRect(ix - 1.5f, iy - 1.5f, 3f, 3f)
        ctx.drawRect(ix - 0.5f, iy - 0.5f, 1f, 1f, Color.BLACK)
    }

    private fun drawVerticalHueBar(ctx: GuiGraphicsExtractor, hx: Float, hy: Float, hw: Float, hh: Float) {
        val segments = 12
        val step = hh / segments.toFloat()

        for (i in 0 until segments) {
            val c1 = Color.getHSBColor(i / segments.toFloat(), 1f, 1f)
            val c2 = Color.getHSBColor((i + 1) / segments.toFloat(), 1f, 1f)
            val yStart = hy + (i * step)
            val yEnd = hy + ((i + 1) * step)
            ctx.drawVerticalGradient(hx, yStart, hw, (yEnd - yStart) + 0.5f, c1, c2)
        }

        ctx.drawRect(hx - 1f, hy + (h * hh) - 0.5f, hw + 2f, 1f)
    }

    private fun drawVerticalAlphaBar(ctx: GuiGraphicsExtractor, ax: Float, ay: Float, aw: Float, ah: Float) {
        drawCheckerboard(ctx, ax, ay, aw, ah, 2)
        val base = Color(Color.HSBtoRGB(h, s, b))
        val cTop = base.withAlpha(255)
        val cBot = base.withAlpha(0)

        ctx.drawVerticalGradient(ax, ay, aw, ah, cTop, cBot)
        ctx.drawRect(ax - 1f, ay + ((1f - a) * ah) - 0.5f, aw + 2f, 1f)
    }

    private fun drawCheckerboard(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, size: Int) {
        for (i in 0 until (w / size).toInt()) {
            for (j in 0 until (h / size).toInt()) {
                val color = if ((i + j) % 2 == 0) Color(50, 50, 50, 200) else Color(30, 30, 30, 200)
                ctx.drawRect(x + i * size, y + j * size, size.toFloat(), size.toFloat(), color)
            }
        }
    }

    override fun clearFocus() {
        hexInput.resetState()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                expanded = ! expanded
                if (! expanded) clearFocus()
                return true
            }
        }
        if (expanded && button == InputConstants.MOUSE_BUTTON_LEFT) {
            val py = y + 25f
            val ps = 80f
            var currentX = x + 10f
            val aX = currentX
            if (cfg.withAlpha) currentX += 15f
            val hX = currentX
            currentX += 15f
            val svX = currentX
            val svW = (x + width - 10f) - svX

            draggingAlpha = cfg.withAlpha && mouseX >= aX && mouseX <= aX + 10 && mouseY >= py && mouseY <= py + ps
            draggingHue = mouseX >= hX && mouseX <= hX + 10 && mouseY >= py && mouseY <= py + ps
            draggingSV = mouseX >= svX && mouseX <= svX + svW && mouseY >= py && mouseY <= py + ps
            if (draggingAlpha || draggingHue || draggingSV) {
                clearFocus()
                return true
            }
            val event = MouseButtonEvent(mouseX, mouseY, MouseButtonInfo(button, 0))
            val inputX = if (mouseX >= x + 10 && mouseX < hexInput.x) hexInput.x.toDouble() else mouseX
            if (hexInput.mouseClicked(inputX.toFloat(), mouseY.toFloat(), event)) return true
        }
        return false
    }

    override fun mouseReleased(button: Int) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            hexInput.mouseReleased()
            draggingSV = false
            draggingHue = false
            draggingAlpha = false
        }
    }
}