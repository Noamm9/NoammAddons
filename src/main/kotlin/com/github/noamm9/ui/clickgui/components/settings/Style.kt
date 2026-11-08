package com.github.noamm9.ui.clickgui.components.settings

import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.ui.clickgui.CheatIndicator
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.NumbersUtils.div
import com.github.noamm9.utils.NumbersUtils.minus
import com.github.noamm9.utils.NumbersUtils.plus
import com.github.noamm9.utils.SoundUtils
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
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

    /** Draws a setting's name (sliding right on hover like [drawNudgedText]) followed by its cheat badge, if it has one. */
    fun drawLabel(ctx: GuiGraphicsExtractor, widget: Widget<*>, x: Float, y: Float, anim: Float, color: Color = Color.WHITE) {
        drawNudgedText(ctx, widget.name, x, y, anim, color)
        drawCheatBadge(ctx, widget, x + 2f * anim + widget.name.width() + 6f, y - 0.5f)
    }

    /**
     * Draws the cheat badge for [widget] with its top-left corner at ([x], [y]) if the setting is marked as a cheat,
     * and remembers the position so the settings window can show the badge tooltip.
     * Use this directly only for labels not drawn through [drawLabel] (e.g. centered labels).
     */
    fun drawCheatBadge(ctx: GuiGraphicsExtractor, widget: Widget<*>, x: Float, y: Float) {
        if (! widget.config.cheat) {
            widget.cheatBadgeX = Float.NaN
            return
        }
        CheatIndicator.Setting.draw(ctx, x, y)
        widget.cheatBadgeX = x
        widget.cheatBadgeY = y
    }

    fun drawSlider(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, progress: Float, color: Color) {
        val kSize = 4f
        val h = 2.5f
        ctx.drawRect(x, y, w, h, sliderBackgroundColor)
        ctx.drawRect(x, y, w * progress, h, color)
        ctx.drawRect(x + (w * progress) - (kSize / 2f), y + (h / 2f) - (kSize / 2f), kSize, kSize)
    }

    fun drawRangeSlider(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, start: Float, end: Float, color: Color) {
        val kSize = 4f
        val h = 2.5f
        ctx.drawRect(x, y, w, h, sliderBackgroundColor)
        ctx.drawRect(x + (w * start), y, w * (end - start), h, color)
        ctx.drawRect(x + (w * start) - (kSize / 2f), y + (h / 2f) - (kSize / 2f), kSize, kSize)
        ctx.drawRect(x + (w * end) - (kSize / 2f), y + (h / 2f) - (kSize / 2f), kSize, kSize)
    }

    fun playClickSound(pitch: Float) = SoundUtils.playSound(SoundEvents.UI_BUTTON_CLICK, 0.25f, pitch)
}