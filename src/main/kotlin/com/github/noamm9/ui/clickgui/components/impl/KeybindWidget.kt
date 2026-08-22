package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.config.types.KeybindSetting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.components.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.UKeyboard
import net.minecraft.client.gui.GuiGraphicsExtractor

class KeybindWidget(config: KeybindSetting): Widget<Int>(config) {
    private inline val cfg get() = config as KeybindSetting

    private val hoverAnim = Animation(200)
    private var listening = false

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x, y, width, height)
        Style.drawHoverBar(ctx, x, y, height, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val bindText = when {
            listening -> "§b..."
            else -> "§7${cfg.displayName()}"
        }
        ctx.drawString(bindText, x + width - bindText.width() - 8f, y + 6f)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val isInside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height

        if (listening) {
            this.value = button
            cfg.isMouse = true
            cfg.scanCode = 0
            this.listening = false
            Style.playClickSound(1f)
            return true
        }

        if (isInside) {
            listening = true
            Style.playClickSound(1f)
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (listening) {
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                listening = false
                return true
            }

            if (keyCode == UKeyboard.KEY_BACKSPACE) {
                value = UKeyboard.KEY_NONE
                cfg.isMouse = false
            }
            else {
                this.value = keyCode
                cfg.scanCode = scanCode
                cfg.isMouse = false
            }
            listening = false
            return true
        }
        return false
    }
}
