package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class ButtonWidget(config: ButtonSetting): Widget<Unit>(config) {
    private inline val cfg get() = config as ButtonSetting

    private val hoverAnim = Animation(200)

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

        if (hoverAnim.value > 0.01f) {
            ctx.drawRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), Color(255, 255, 255, (10 * hoverAnim.value).toInt()))
        }

        ctx.drawCenteredString(name, x + (width / 2), y + 6)

        if (hoverAnim.value > 0.01f) {
            val textWidth = name.width()
            val maxLineWidth = (textWidth + 10).toFloat()
            val currentLineWidth = maxLineWidth * hoverAnim.value

            val lineX = x + (width / 2f) - (currentLineWidth / 2f)
            val lineY = y + 15.5f

            ctx.drawRect(lineX, lineY, currentLineWidth, 1f, Style.accentColor.withAlpha((200 * hoverAnim.value).toInt()))
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (cfg.playSound) Style.playClickSound(1f)
            cfg.action.invoke()
            return true
        }
        return false
    }
}