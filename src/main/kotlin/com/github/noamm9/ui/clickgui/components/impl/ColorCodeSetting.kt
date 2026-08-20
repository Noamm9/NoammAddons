package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.config.types.ColorCodeConfig
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class ColorCodeSetting(config: ColorCodeConfig): Setting<ChatFormatting>(config) {
    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    private val cols = 8
    private val rows = 2
    private val cellH = 14f

    override val height get() = 20 + (openAnim.value * 48).toInt()

    private fun swatchColor(format: ChatFormatting) = Color(format.color ?: 0xFFFFFF)

    private fun prettyName(format: ChatFormatting) =
        format.name.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x.toFloat(), y.toFloat(), width.toFloat(), 20f)
        Style.drawHoverBar(ctx, x.toFloat(), y.toFloat(), 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        ctx.drawRect(x + width - 18f, y + 6f, 8f, 8f, swatchColor(value))

        ctx.enableScissor(x, y, x + width, y + height)

        if (expanded) {
            val gridX = x + 10f
            val gridW = width - 20f
            val cellW = gridW / cols
            val gridY = y + 24f
            var hoveredName: String? = null

            ColorCodeConfig.COLORS.forEachIndexed { index, format ->
                val col = index % cols
                val row = index / cols
                val cx = gridX + col * cellW
                val cy = gridY + row * (cellH + 2f)

                val hov = mouseX >= cx && mouseX <= cx + cellW - 2f && mouseY >= cy && mouseY <= cy + cellH
                if (hov) hoveredName = prettyName(format)

                ctx.drawRect(cx, cy, cellW - 2f, cellH, swatchColor(format))
                if (format == value) ctx.drawBorder(cx, cy, cellW - 1, cellH)
            }

            val label = hoveredName ?: prettyName(value)
            ctx.drawString(label, gridX, gridY + rows * (cellH + 2f) + 2f, swatchColor(value), 1f)
        }

        ctx.disableScissor()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            if (button == 0) {
                expanded = ! expanded
                Style.playClickSound(1f)
                return true
            }
        }

        if (expanded) {
            val gridX = x + 10f
            val gridW = width - 20f
            val cellW = gridW / cols
            val gridY = y + 24f

            ColorCodeConfig.COLORS.forEachIndexed { index, format ->
                val col = index % cols
                val row = index / cols
                val cx = gridX + col * cellW
                val cy = gridY + row * (cellH + 2f)

                if (mouseX >= cx && mouseX <= cx + cellW - 2f && mouseY >= cy && mouseY <= cy + cellH) {
                    value = format
                    Style.playClickSound(1f)
                    expanded = false
                    return true
                }
            }

            expanded = false
        }

        return false
    }
}