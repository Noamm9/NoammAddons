package com.github.noamm9.ui.hud

import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.reflect.jvm.jvmName

abstract class HudElement {
    open val name = this::class.simpleName ?: this::class.jvmName

    abstract val toggle: Boolean
    open val shouldDraw = true
    open val centered: () -> Boolean = { false }

    var x = defaultX
    var y = defaultY
    var scale = defaultScale

    var width = 0f
    var height = 0f

    var defaults: HudElement.() -> Unit = {
        x = defaultX
        y = defaultY
        scale = defaultScale
    }

    var isDragging = false

    abstract fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Number, Number>

    data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val centerX get() = (left + right) / 2f
        val centerY get() = (top + bottom) / 2f
        fun contains(x: Float, y: Float) = x in left .. right && y in top .. bottom
        fun intersects(other: Bounds) = left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }

    open fun bounds(): Bounds {
        val scaledW = width * scale
        val scaledH = height * scale
        val left = if (centered()) x - scaledW / 2f else x
        return Bounds(left, y, left + scaledW, y + scaledH)
    }

    open fun drawBackground(ctx: GuiGraphicsExtractor, mx: Int, my: Int) {
        val scaledW = width * scale
        val scaledH = height * scale
        val centeredOffset = if (centered()) scaledW / 2f else 0f
        val hovered = mx >= x - centeredOffset && mx <= x - centeredOffset + scaledW && my >= y && my <= y + scaledH

        val borderColor = if (isDragging || hovered) Style.accentColor else Color(255, 255, 255, 40)

        ctx.drawRect(x - centeredOffset, y, scaledW, scaledH, Color(10, 10, 10, 150))
        ctx.drawRect(x - centeredOffset, y, scaledW, 1f, borderColor)
        ctx.drawRect(x - centeredOffset, y + scaledH - 1f, scaledW, 1f, borderColor)
    }

    fun renderElement(ctx: GuiGraphicsExtractor, example: Boolean) {
        ctx.pose().pushMatrix()
        ctx.pose().translate(x, y)
        ctx.pose().scale(scale, scale)

        draw(ctx, example).run {
            width = first.toFloat()
            height = second.toFloat()
        }

        ctx.pose().popMatrix()
    }

    open fun isHovered(mx: Int, my: Int): Boolean {
        val scaledW = width * scale
        val scaledH = height * scale
        val centeredOffset = (if (centered()) scaledW / 2 else 0).toInt()
        return mx >= x - centeredOffset && mx <= x - centeredOffset + scaledW && my >= y && my <= y + scaledH
    }

    companion object {
        const val defaultX = 20f
        const val defaultY = 20f
        const val defaultScale = 1f
    }
}