package com.github.noamm9.ui.clickgui.components.settings

import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.NumbersUtils.div
import com.github.noamm9.utils.NumbersUtils.minus
import com.github.noamm9.utils.NumbersUtils.plus
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import gg.essential.universal.USound
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import java.awt.Color

object Style {
    val accentColor get() = ClickGui.accentColor.value
    val accentColorTrans get() = accentColor.withAlpha(120)
    val settingBackgroundColor = Color(10, 10, 10, 100)
    val sliderBackgroundColor = Color(40, 40, 40, 200)

    fun drawBackground(ctx: GuiGraphicsExtractor, x: Number, y: Number, w: Number, h: Number) {
        ctx.drawRect(x, y, w, h, settingBackgroundColor)
    }

    fun drawHoverBar(ctx: GuiGraphicsExtractor, x: Number, y: Number, height: Number, anim: Float) {
        if (anim <= 0.01f) return
        val barH = (height - 6f) * anim
        val barY = y + (height / 2f) - (barH / 2f)
        ctx.drawRect(x, barY, 1.5f, barH, accentColor.withAlpha((200 * anim).toInt()))
    }

    fun drawNudgedText(ctx: GuiGraphicsExtractor, text: String, x: Float, y: Float, anim: Float, color: Color = Color.WHITE) {
        val xOffset = 2f * anim
        ctx.drawString(text, x + xOffset, y, color)
    }

    fun drawSlider(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, progress: Float, hoverAnim: Float, color: Color) {
        val barColor = MathUtils.lerpColor(color.withAlpha(180), color, hoverAnim)
        val kSize = 4f
        val h = 2.5f
        ctx.drawRect(x, y, w, h, sliderBackgroundColor)
        ctx.drawRect(x, y, w * progress, h, barColor)
        ctx.drawRect(x + (w * progress) - (kSize / 2f), y + (h / 2f) - (kSize / 2f), kSize, kSize)
    }

    fun playClickSound(pitch: Float) = USound.playSoundStatic(SoundEvents.UI_BUTTON_CLICK, 0.25f, pitch)
}