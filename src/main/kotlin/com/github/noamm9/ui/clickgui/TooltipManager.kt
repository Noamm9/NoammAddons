package com.github.noamm9.ui.clickgui

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.abs

object TooltipManager {
    private var hoveredText: String? = null
    private var displayStartTime = 0L

    private var lastMouseX = 0
    private var lastMouseY = 0
    private const val displayDelay = 600L

    fun reset() {
        hoveredText = null
    }

    fun hover(text: String?, mouseX: Int, mouseY: Int) {
        if (text.isNullOrEmpty()) return
        if (abs(mouseX - lastMouseX) > 5 || abs(mouseY - lastMouseY) > 5) {
            displayStartTime = System.currentTimeMillis()
        }

        lastMouseX = mouseX
        lastMouseY = mouseY
        hoveredText = text
    }

    fun draw(context: GuiGraphicsExtractor, logicalWidth: Float, logicalHeight: Float) {
        val text = hoveredText ?: return
        if (System.currentTimeMillis() - displayStartTime < displayDelay) return

        val lines = mc.font.split(Component.literal(text.addColor()), 150)
        val padding = 6
        val textWidth = lines.maxOfOrNull { mc.font.width(it) } ?: return
        val textHeight = lines.size * (mc.font.lineHeight + 2)

        var tx = lastMouseX + 12f
        var ty = lastMouseY + 12f

        if (tx + textWidth + (padding * 2) > logicalWidth) tx = lastMouseX - textWidth - (padding * 2) - 4f
        if (ty + textHeight + (padding * 2) > logicalHeight) ty = logicalHeight - textHeight - (padding * 2) - 4f

        Render2D.drawRect(context, tx, ty, textWidth + (padding * 2f), textHeight + (padding * 2f), Color(10, 10, 10, 240))
        Render2D.drawRect(context, tx, ty, textWidth + (padding * 2f), 1.5f, Style.accentColor)

        var currentY = ty + padding
        lines.forEach { line ->
            context.text(mc.font, line, (tx + padding).toInt(), currentY.toInt(), - 1, true)
            currentY += mc.font.lineHeight + 2
        }
    }
}