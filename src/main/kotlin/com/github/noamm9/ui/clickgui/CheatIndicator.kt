package com.github.noamm9.ui.clickgui

import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

/**
 * Small red warning triangle drawn next to features and settings that are marked as cheats
 * (see `Feature.cheat` and `ConfigHolder.cheat`). Hovering it shows a tooltip.
 *
 * [Feature] and [Setting] are the same icon with different tooltip wording.
 */
sealed class CheatIndicator(override val tooltip: String): FeatureBadge {
    object Feature: CheatIndicator("§c§lCheat§r\nThis feature is marked as a cheat. Using it may break the server rules and can get you banned.")
    object Setting: CheatIndicator("§c§lCheat§r\nThis option is marked as a cheat. Using it may break the server rules and can get you banned.")

    override val width = WIDTH
    override val height = HEIGHT

    override fun draw(ctx: GuiGraphicsExtractor, x: Float, y: Float) {
        val rows = HEIGHT.toInt()
        for (i in 0 until rows) {
            val rowWidth = WIDTH * (i + 1) / rows
            ctx.drawRect(x + (WIDTH - rowWidth) / 2f, y + i, rowWidth, 1f, fillColor)
        }

        val markX = x + WIDTH / 2f - 0.5f
        ctx.drawRect(markX, y + 3f, 1f, 3f, markColor)
        ctx.drawRect(markX, y + 7f, 1f, 1f, markColor)
    }

    companion object {
        const val WIDTH = 10f
        const val HEIGHT = 9f

        private val fillColor = Color(220, 45, 45)
        private val markColor = Color(25, 10, 10)
    }
}
