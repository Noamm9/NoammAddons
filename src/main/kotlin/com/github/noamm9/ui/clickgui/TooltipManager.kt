package com.github.noamm9.ui.clickgui

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.RenderHelper.wrap
import gg.essential.universal.UGraphics
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.abs

object TooltipManager {
    private const val displayDelay = 500L
    private val backgroundColor = Color(10, 10, 10, 240)

    private var hoveredText: String? = null
    private var displayStartTime = 0L
    private var lastMouseX = 0
    private var lastMouseY = 0

    fun hover(text: String?, mouseX: Int, mouseY: Int) {
        if (text.isNullOrBlank()) return
        if (abs(mouseX - lastMouseX) > 5 || abs(mouseY - lastMouseY) > 5) {
            displayStartTime = System.currentTimeMillis()
        }

        lastMouseX = mouseX
        lastMouseY = mouseY
        hoveredText = text
    }

    fun draw(context: GuiGraphicsExtractor, logicalWidth: Float = Resolution.width, logicalHeight: Float = Resolution.height) {
        val text = hoveredText ?: return
        if (System.currentTimeMillis() - displayStartTime < displayDelay) return

        val lines = text.wrap(150)
        val padding = 6
        val textWidth = lines.maxOf { it.width() }
        val textHeight = lines.size * (UGraphics.getFontHeight() + 2)

        var tx = lastMouseX + 12f
        var ty = lastMouseY + 12f

        if (tx + textWidth + (padding * 2) > logicalWidth) tx = lastMouseX - textWidth - (padding * 2) - 4f
        if (ty + textHeight + (padding * 2) > logicalHeight) ty = logicalHeight - textHeight - (padding * 2) - 4f

        context.drawRect(tx, ty, textWidth + (padding * 2f), textHeight + (padding * 1.5f), backgroundColor)
        context.drawRect(tx, ty, textWidth + (padding * 2f), 1.5f, Style.accentColor)

        var currentY = ty + padding
        lines.forEach { line ->
            context.text(mc.font, line, (tx + padding).toInt(), currentY.toInt(), - 1, true)
            currentY += mc.font.lineHeight + 2
        }

        hoveredText = null
    }
}