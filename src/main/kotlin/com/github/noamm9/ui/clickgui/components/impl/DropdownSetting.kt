package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.config.Savable
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class DropdownSetting(name: String, value: Int = 0, val options: List<String>): Setting<Int>(name, value), Savable {
    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    override val height get() = 20 + (openAnim.value * (options.size * 16)).toInt()

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Render2D.drawRect(ctx, x, y, width, 20f, Style.bg)
        Style.drawHoverBar(ctx, x, y, 20f, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val valStr = "§7${options[value]}"
        Render2D.drawString(ctx, valStr, x + width - valStr.width() - 8f, y + 6f, Color.WHITE, 1f)

        ctx.enableScissor(x, y, x + width, y + height)

        if (expanded) {
            var oy = y + 20f
            Render2D.drawRect(ctx, x + 4f, oy, width - 8f, (options.size * 16) * openAnim.value, Color(5, 5, 5, 150))
            options.forEachIndexed { index, opt ->
                val hov = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= oy && mouseY <= oy + 16
                if (hov) Render2D.drawRect(ctx, x + 4f, oy, width - 8f, 16f, Color(255, 255, 255, 20))
                if (index == value) Render2D.drawRect(ctx, x + 4f, oy + 2f, 1.5f, 12f, Style.accentColor)

                val color = if (index == value) Style.accentColor else if (hov) Color.WHITE else Color.GRAY
                Render2D.drawString(ctx, opt, x + 12f, oy + 4f, color, 1f)
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
            options.forEachIndexed { index, option ->
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

    override fun write() = JsonPrimitive(value)
    override fun read(element: JsonElement?) {
        (element as? JsonPrimitive)?.intOrNull?.let {
            value = it.coerceIn(0, options.lastIndex)
        }
    }
}