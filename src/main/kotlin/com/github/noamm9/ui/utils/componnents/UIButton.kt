package com.github.noamm9.ui.utils.componnents

import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import java.awt.Color

class UIButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    text: String,
    private val colorProvider: (() -> Color)? = null,
    private val action: (UIButton) -> Unit
): Button(x, y, width, height, Component.literal(text.addColor()), { btn -> action(btn as UIButton) }, DEFAULT_NARRATION) {

    var overrideColor: Color? = null

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val bgColor = if (isHovered) Color(45, 45, 45, 220) else Color(30, 30, 30, 200)
        Render2D.drawRect(context, x, y, width, height, bgColor)

        val stateColor = colorProvider?.invoke() ?: overrideColor
        val borderColor = stateColor ?: Color(60, 60, 60)
        val textColor = if (isHovered) Style.accentColor else stateColor ?: Color.WHITE

        Render2D.drawRect(context, x, y, width, 1, borderColor)
        Render2D.drawRect(context, x, y + height - 1, width, 1, borderColor)
        Render2D.drawRect(context, x, y, 1, height, borderColor)
        Render2D.drawRect(context, x + width - 1, y, 1, height, borderColor)

        context.drawCenteredString(
            Minecraft.getInstance().font,
            message,
            x + width / 2,
            y + (height - 8) / 2,
            textColor.rgb
        )
    }

    override fun playDownSound(soundManager: SoundManager) {
        Style.playClickSound(1f)
    }
}

