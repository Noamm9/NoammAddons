package com.github.noamm9.ui.clickgui

import com.github.noamm9.features.Feature
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * A small icon shown next to a feature in the ClickGui (category rows and the settings window title).
 * Hovering it shows [tooltip], which takes priority over the feature description.
 *
 * To add a new kind of badge, implement this and add a condition to [FeatureBadges.of].
 */
interface FeatureBadge {
    val width: Float
    val height: Float
    val tooltip: String

    /** Draws the badge with its top-left corner at ([x], [y]). */
    fun draw(ctx: GuiGraphicsExtractor, x: Float, y: Float)

    fun isHovered(x: Float, y: Float, mouseX: Int, mouseY: Int) =
        mouseX >= x - 1 && mouseX <= x + width + 1 && mouseY >= y - 1 && mouseY <= y + height + 1

    /** Draws the badge and, when [hoverable], queues its tooltip if the mouse is over it. */
    fun render(ctx: GuiGraphicsExtractor, x: Float, y: Float, mouseX: Int, mouseY: Int, hoverable: Boolean = true) {
        draw(ctx, x, y)
        if (hoverable && isHovered(x, y, mouseX, mouseY)) {
            TooltipManager.hover(tooltip, mouseX, mouseY, TooltipManager.PRIORITY_BADGE)
        }
    }
}

object FeatureBadges {
    const val GAP = 3f

    /** Badges for [feature], in display order (left to right). */
    fun of(feature: Feature): List<FeatureBadge> = buildList {
        if (feature.cheat) add(CheatIndicator.Feature)
    }

    /** Total width of [badges] laid out in a row, including the gaps between them. */
    fun rowWidth(badges: List<FeatureBadge>) =
        if (badges.isEmpty()) 0f else badges.sumOf { it.width.toDouble() }.toFloat() + GAP * (badges.size - 1)

    /**
     * Draws [badges] left to right starting at [x], each vertically centered in a row of [rowHeight] starting at [rowY].
     * Returns the x coordinate just after the last badge.
     */
    fun renderRow(ctx: GuiGraphicsExtractor, badges: List<FeatureBadge>, x: Float, rowY: Float, rowHeight: Float, mouseX: Int, mouseY: Int, hoverable: Boolean = true): Float {
        var bx = x
        badges.forEachIndexed { index, badge ->
            if (index > 0) bx += GAP
            badge.render(ctx, bx, rowY + (rowHeight - badge.height) / 2f, mouseX, mouseY, hoverable)
            bx += badge.width
        }
        return bx
    }
}
