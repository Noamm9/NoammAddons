package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.MultiCheckboxSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.scissor
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class MultiCheckboxWidget(config: MultiCheckboxSetting): Widget<MutableMap<String, Boolean>>(config) {
    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    override val height get() = 20 + (openAnim.value * (value.size * 16)).toInt()

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x.toFloat(), y.toFloat(), width.toFloat(), 20f)
        Style.drawHoverBar(ctx, x.toFloat(), y.toFloat(), 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val icon = if (expanded) "§7-" else "§7+"
        ctx.drawString(icon, x + width - 15f, y + 6f)

        ctx.scissor(x, y, width, height)

        if (expanded) {
            var oy = y + 20f
            ctx.drawRect(x + 4f, oy, width - 8f, (value.size * 16) * openAnim.value, Color(5, 5, 5, 150))
            value.forEach { (optionName, isEnabled) ->
                val hov = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= oy && mouseY <= oy + 16

                if (hov) ctx.drawRect(x + 4f, oy, width - 8f, 16f, Color(255, 255, 255, 20))
                if (isEnabled) ctx.drawRect(x + 4f, oy + 2f, 1.5f, 12f, Style.accentColor)

                val color = if (isEnabled) Style.accentColor else if (hov) Color.WHITE else Color.GRAY
                ctx.drawString(optionName, x + 12f, oy + 4f, color)

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

        if (expanded) {
            var currentOy = y + 20
            value.keys.toList().forEach { optionKey ->
                if (mouseX >= x && mouseX <= x + width && mouseY >= currentOy && mouseY <= currentOy + 16) {
                    if (button == 0) {
                        value[optionKey] = ! (value[optionKey] ?: false)
                        Style.playClickSound(if (value[optionKey] == true) 1.1f else 0.9f)
                        return true
                    }
                }
                currentOy += 16
            }
        }

        return false
    }
}