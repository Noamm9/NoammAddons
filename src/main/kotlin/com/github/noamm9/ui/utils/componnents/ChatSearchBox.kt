package com.github.noamm9.ui.utils.componnents

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import java.awt.Color

/**
 * The search bar that gets opened above the chat input with Ctrl + F.
 * Mirrors the look of the vanilla chat input, with the match count drawn on the right.
 */
class ChatSearchBox(x: Int, y: Int, width: Int, height: Int): EditBox(mc.font, x, y, width, height, Component.literal("Chat Search")) {
    /** Text drawn on the right side of the bar, usually the amount of matches. */
    var status: () -> String = { "" }

    init {
        isBordered = false
        setMaxLength(256)
        setTextColor(Color.WHITE.rgb)
        setHint(Component.literal("Search chat... (Esc to close)"))
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (! this.isVisible) return

        guiGraphics.drawRect(2f, (y - 1).toFloat(), (width + 2).toFloat(), (height + 2).toFloat(), Color(0, 0, 0, 140))
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick)

        val status = status()
        if (status.isEmpty()) return
        guiGraphics.drawString(status, x + width - status.width() - 4f, y + height / 2f - 4f, Color(170, 170, 170))
    }
}
