package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.scissor
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class DropdownWidget(config: DropdownSetting): Widget<Int>(config) {
    private inline val cfg get() = config as DropdownSetting

    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    override val height get() = 20 + (openAnim.value * (cfg.options.size * 16)).toInt()

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        ctx.drawRect(x, y, width, 20f, Style.settingBackgroundColor)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val valStr = "§7${cfg.options[value]}"
        ctx.drawString(valStr, x + width - valStr.width() - 8f, y + 6f, scale = 1f)

        ctx.scissor(x, y, width, height)

        if (expanded) {
            var oy = y + 20f
            ctx.drawRect(x + 4f, oy, width - 8f, (cfg.options.size * 16) * openAnim.value, Color(5, 5, 5, 150))
            cfg.options.forEachIndexed { index, opt ->
                val hov = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= oy && mouseY <= oy + 16
                if (hov) ctx.drawRect(x + 4f, oy, width - 8f, 16f, Color(255, 255, 255, 20))
                if (index == value) ctx.drawRect(x + 4f, oy + 2f, 1.5f, 12f, Style.accentColor)

                val color = if (index == value) Style.accentColor else if (hov) Color.WHITE else Color.GRAY
                ctx.drawString(opt, x + 12f, oy + 4f, color, 1f)
                oy += 16
            }
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

        if (expanded && mouseX >= x && mouseX <= x + width && mouseY >= y + 20 && mouseY <= y + height) {
            var optionY = y + 20
            cfg.options.forEachIndexed { index, _ ->
                if (mouseX >= x && mouseX <= x + width && mouseY >= optionY && mouseY <= optionY + 16) {
                    value = index
                    Style.playClickSound(1f)
                    expanded = false
                    return true
                }
                optionY += 16
            }
        }

        if (expanded) expanded = false
        return false
    }
}