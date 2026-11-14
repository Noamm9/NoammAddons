package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ColorUtils.lerp
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.scissor
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

@Suppress("UNCHECKED_CAST")
class GroupWidget(private val parent: Widget<*>, private val children: List<Widget<*>>): Widget<Any?>(parent.config as ConfigHolder<Any?>) {
    private val visibleChildren get() = children.filter { it.config.visibility() }

    private var expanded = false
    private val openAnim = Animation(250)
    private val hoverAnim = Animation(200)

    private val gutter = 18
    private val buttonSize = 10f
    private val childSpacing = 2

    private val childrenHeight get() = visibleChildren.sumOf { it.height + childSpacing }
    override val height get() = parent.height + (openAnim.value * childrenHeight).toInt()

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        openAnim.update(if (expanded) 1f else 0f)
        hoverAnim.update(if (isOverButton(mouseX.toDouble(), mouseY.toDouble())) 1f else 0f)

        ctx.drawRect(x, y, gutter, 20f, Style.settingBackgroundColor)
        drawExpandButton(ctx)

        parent.x = x + gutter
        parent.y = y
        parent.width = width - gutter
        parent.draw(ctx, mouseX, mouseY)

        if (openAnim.value <= 0.01f) return

        val top = y + parent.height
        val lineX = x + gutter / 2f - 0.5f
        ctx.scissor(x, top, width, height - parent.height)
        ctx.drawRect(lineX, top, 1f, height - parent.height - childSpacing, Style.accentColor.withAlpha(120))

        var childY = top + childSpacing
        visibleChildren.forEach { child ->
            ctx.drawRect(lineX, childY + 10f, gutter / 2f - 2f, 1f, Style.accentColor.withAlpha(120))

            child.x = x + gutter
            child.y = childY
            child.width = width - gutter
            child.draw(ctx, mouseX, mouseY)
            childY += child.height + childSpacing
        }

        ctx.disableScissor()
    }

    private fun drawExpandButton(ctx: GuiGraphicsExtractor) {
        val bx = x + (gutter - buttonSize) / 2f
        val by = y + (20f - buttonSize) / 2f
        val background = Color(40, 40, 40, 180).lerp(Style.accentColor.withAlpha(60), openAnim.value)
        val iconColor = Color.GRAY.lerp(Color.WHITE, hoverAnim.value).lerp(Style.accentColor, openAnim.value)

        ctx.drawRect(bx, by, buttonSize, buttonSize, background)
        if (hoverAnim.value > 0.01f) ctx.drawBorder(bx, by, buttonSize, buttonSize, Color(255, 255, 255, (60 * hoverAnim.value).toInt()))

        val center = buttonSize / 2f
        ctx.drawRect(bx + 2f, by + center - 0.5f, buttonSize - 4f, 1f, iconColor)
        val barHeight = (buttonSize - 4f) * (1f - openAnim.value)
        ctx.drawRect(bx + center - 0.5f, by + center - barHeight / 2f, 1f, barHeight, iconColor)
    }

    private fun isOverButton(mouseX: Double, mouseY: Double) = mouseX >= x && mouseX <= x + gutter && mouseY >= y && mouseY <= y + 20

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val onHeader = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20
        if (isOverButton(mouseX, mouseY) || (onHeader && button == InputConstants.MOUSE_BUTTON_RIGHT)) {
            expanded = ! expanded
            Style.playClickSound(1f)
            return true
        }

        if (parent.mouseClicked(mouseX, mouseY, button)) return true
        if (! expanded) return false
        return visibleChildren.any { it.mouseClicked(mouseX, mouseY, button) }
    }

    override fun description(mouseX: Int, mouseY: Int): String? {
        if (! expanded || mouseY < y + parent.height || mouseY > y + height) return parent.description(mouseX, mouseY)
        val hovered = visibleChildren.find { mouseX >= it.x && mouseX <= it.x + it.width && mouseY >= it.y && mouseY <= it.y + it.height }
        return hovered?.description(mouseX, mouseY)
    }

    override fun clearFocus() {
        parent.clearFocus()
        children.forEach { it.clearFocus() }
    }

    override fun mouseReleased(button: Int) {
        parent.mouseReleased(button)
        children.forEach { it.mouseReleased(button) }
    }

    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        if (parent.mouseScrolled(mouseX, mouseY, delta)) return true
        return expanded && visibleChildren.any { it.mouseScrolled(mouseX, mouseY, delta) }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (parent.keyPressed(keyCode, scanCode, modifiers)) return true
        return expanded && visibleChildren.any { it.keyPressed(keyCode, scanCode, modifiers) }
    }

    override fun charTyped(codePoint: Char): Boolean {
        if (parent.charTyped(codePoint)) return true
        return expanded && visibleChildren.any { it.charTyped(codePoint) }
    }
}
