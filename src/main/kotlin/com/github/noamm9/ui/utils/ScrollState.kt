package com.github.noamm9.ui.utils

import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class ScrollState(
    duration: Long = 200L,
    private val minThumbHeight: Float = 20f,
    private val fixedThumbHeight: Float? = null
) {
    private val anim = Animation(duration)

    var target = 0f
        private set

    var maxScroll = 0f
        private set

    var dragging = false
        private set

    private var dragOffset = 0f
    private var contentHeight = 0f
    private var viewHeight = 0f
    private var barX = 0f
    private var barY = 0f
    private var barW = 0f
    private var barH = 0f

    val value get() = anim.value.coerceIn(0f, maxScroll)
    val canScroll get() = maxScroll > 0f

    val thumbH get() = fixedThumbHeight ?: if (contentHeight <= viewHeight) barH else ((viewHeight / contentHeight) * barH).coerceAtLeast(minThumbHeight)
    val thumbY get() = barY + if (canScroll) (value / maxScroll) * (barH - thumbH) else 0f

    fun update(contentH: Float, viewH: Float) {
        contentHeight = contentH
        viewHeight = viewH
        maxScroll = (contentH - viewH).coerceAtLeast(0f)
        target = target.coerceIn(0f, maxScroll)
        if (! canScroll) dragging = false
        anim.update(target)
    }

    fun scroll(delta: Float) {
        target = (target - delta).coerceIn(0f, maxScroll)
    }

    fun scrollTo(value: Float, instant: Boolean = false) {
        target = value.coerceAtLeast(0f)
        if (instant) anim.set(target)
    }

    fun reset() = scrollTo(0f, true)

    fun isHovered(mx: Float, my: Float) = mx >= barX - 3f && mx <= barX + barW + 3f && my >= barY && my <= barY + barH

    fun drawScrollbar(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, mx: Float, my: Float, trackColor: Color? = null, thumbColor: Color? = null) {
        barX = x
        barY = y
        barW = w
        barH = h

        if (! canScroll) return

        val hovered = isHovered(mx, my)
        ctx.drawRect(x, y, w, h, trackColor ?: Color(255, 255, 255, if (hovered) 24 else 15))
        ctx.drawRect(x, thumbY, w, thumbH, thumbColor ?: Style.accentColor.withAlpha(if (hovered || dragging) 255 else 190))
    }

    fun mouseClicked(mx: Float, my: Float): Boolean {
        if (! canScroll) return false
        if (! isHovered(mx, my)) return false

        val thumbY = thumbY
        val thumbH = thumbH
        val onThumb = my in thumbY .. thumbY + thumbH

        dragOffset = if (onThumb) my - thumbY else thumbH / 2f
        dragging = true
        if (! onThumb) mouseDragged(my)
        return true
    }

    fun mouseDragged(my: Float) {
        if (! dragging) return
        val travel = barH - thumbH
        if (travel <= 0f) return

        target = ((my - barY - dragOffset) / travel).coerceIn(0f, 1f) * maxScroll
        anim.set(target)
    }

    fun mouseReleased() {
        dragging = false
    }
}
