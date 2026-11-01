package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.RangeSliderSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.abs

class RangeSliderWidget<T: Number>(config: RangeSliderSetting<T>): Widget<Pair<T, T>>(config) {
    private inline val cfg get() = config as RangeSliderSetting<T>

    private var draggingStart = false
    private var draggingEnd = false

    private val hoverAnim = Animation(200)
    private val startAnim = Animation(250, cfg.getPercent(config.value.first))
    private val endAnim = Animation(250, cfg.getPercent(config.value.second))

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        val dragging = draggingStart || draggingEnd

        if (dragging) {
            val pct = ((mouseX - (x + 8.0)) / (width - 16.0)).coerceIn(0.0, 1.0)
            val range = cfg.max.toDouble() - cfg.min.toDouble()
            val snapped = cfg.snapToStep(cfg.min.toDouble() + (range * pct))

            value = if (draggingStart) {
                if (snapped.toDouble() > value.second.toDouble()) value.second to value.second
                else snapped to value.second
            }
            else {
                if (snapped.toDouble() < value.first.toDouble()) value.first to value.first
                else value.first to snapped
            }

            val startTarget = cfg.getPercent(value.first)
            val endTarget = cfg.getPercent(value.second)
            if (abs(startAnim.value - startTarget) < 0.05f) startAnim.set(startTarget)
            if (abs(endAnim.value - endTarget) < 0.05f) endAnim.set(endTarget)
        }

        hoverAnim.update(if (isHovered || dragging) 1f else 0f)
        startAnim.update(cfg.getPercent(value.first))
        endAnim.update(cfg.getPercent(value.second))

        Style.drawBackground(ctx, x, y, width, 20f)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 2f, hoverAnim.value)

        val valStr = "${cfg.stringfy(value.first)} - ${cfg.stringfy(value.second)}${cfg.suffix}"
        ctx.drawString(valStr, x + width - valStr.width() - 8f, y + 2f, Color(180, 180, 180))

        Style.drawRangeSlider(ctx, x + 8f, y + 14f, width - 16f, startAnim.value, endAnim.value, Style.accentColor)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false

        val pct = ((mouseX - (x + 8.0)) / (width - 16.0)).coerceIn(0.0, 1.0)
        val startPct = cfg.getPercent(value.first)
        val endPct = cfg.getPercent(value.second)

        val pickStart = if (startPct == endPct) pct <= startPct
        else abs(pct - startPct) <= abs(pct - endPct)

        draggingStart = pickStart
        draggingEnd = ! pickStart
        return true
    }

    override fun mouseReleased(button: Int) {
        draggingStart = false
        draggingEnd = false
    }
}
