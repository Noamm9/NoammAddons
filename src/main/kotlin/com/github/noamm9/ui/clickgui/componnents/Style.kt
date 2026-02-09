package com.github.noamm9.ui.clickgui.componnents

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.NumbersUtils.div
import com.github.noamm9.utils.NumbersUtils.minus
import com.github.noamm9.utils.NumbersUtils.plus
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import java.awt.Color

object Style {
    val accentColor get() = ClickGui.accsentColor.value
    val accentColorTrans get() = accentColor.withAlpha(120)
    val bg = Color(10, 10, 10, 100)

    fun drawBackground(ctx: GuiGraphics, x: Number, y: Number, w: Number, h: Number) {
        Render2D.drawRect(ctx, x, y, w, h, bg)
    }

    fun drawHoverBar(ctx: GuiGraphics, x: Number, y: Number, height: Number, anim: Float) {
        if (anim <= 0.01f) return
        val barH = (height - 6f) * anim
        val barY = y + (height / 2f) - (barH / 2f)
        Render2D.drawRect(ctx, x, barY, 1.5f, barH, accentColor.withAlpha((200 * anim).toInt()))
    }

    fun drawNudgedText(ctx: GuiGraphics, text: String, x: Float, y: Float, anim: Float, color: Color = Color.WHITE) {
        val xOffset = 2f * anim
        Render2D.drawString(ctx, text, x + xOffset, y, color, 1, true)
    }

    fun drawSlider(ctx: GuiGraphics, x: Float, y: Float, w: Float, progress: Float, hoverAnim: Float, color: Color) {
        val h = 2.5f
        Render2D.drawRect(ctx, x, y, w, h, Color(40, 40, 40, 200))
        val barColor = MathUtils.lerpColor(Color(color.red, color.green, color.blue, 180), color, hoverAnim)
        Render2D.drawRect(ctx, x, y, w * progress, h, barColor)
        val kSize = 4f
        Render2D.drawRect(ctx, x + (w * progress) - (kSize / 2f), y + (h / 2f) - (kSize / 2f), kSize, kSize, Color.WHITE)
    }

    fun playClickSound(pitch: Float) {
        if (! ClickGui.playClickSound.value) return
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch))
    }
}