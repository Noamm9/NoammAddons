package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class ButtonSetting(name: String, val playSound: Boolean = true, val action: () -> Unit): Setting<Unit>(name, Unit) {
    private val hoverAnim = Animation(200)

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

        if (hoverAnim.value > 0.01f) {
            Render2D.drawRect(ctx, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), Color(255, 255, 255, (10 * hoverAnim.value).toInt()))
        }

        Render2D.drawCenteredString(ctx, name, x + (width / 2), y + 6, Color.WHITE)

        if (hoverAnim.value > 0.01f) {
            val textWidth = with(Render2D) { name.width() }

            val maxLineWidth = (textWidth + 10).toFloat()
            val currentLineWidth = maxLineWidth * hoverAnim.value

            val lineX = x + (width / 2f) - (currentLineWidth / 2f)
            val lineY = y + 15.5f

            Render2D.drawRect(ctx, lineX, lineY, currentLineWidth, 1f, Style.accentColor.withAlpha((200 * hoverAnim.value).toInt()))
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (playSound) Style.playClickSound(1f)
            action.invoke()
            return true
        }
        return false
    }
}