package com.github.noamm9.features.impl.dungeon.leapmenu

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawFloatingRect
import com.github.noamm9.utils.render.Render2D.drawPlayerHead
import com.github.noamm9.utils.render.Render2D.drawString
import net.minecraft.client.gui.GuiGraphicsExtractor

object GridLeapRenderer: LeapMenuRenderer() {
    private const val boxWidth = 128f * 1.3f
    private const val boxHeight = 80f * 0.8f
    private const val padding = 40f
    private const val headSize = 50

    override fun render(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        Resolution.push(ctx)
        val userScale = (LeapMenu.scale.value.toFloat() / 100f) * 2.0f
        val screenWidth = Resolution.width / userScale
        val screenHeight = Resolution.height / userScale

        val pose = ctx.pose()
        pose.pushMatrix()
        pose.scale(userScale)

        val gridW = (boxWidth * 2) + padding
        val gridH = (boxHeight * 2) + padding
        val startX = (screenWidth - gridW) / 2f
        val startY = (screenHeight - gridH) / 2f

        val offsets = listOf(
            startX to startY,
            (startX + boxWidth + padding) to startY,
            startX to (startY + boxHeight + padding),
            (startX + boxWidth + padding) to (startY + boxHeight + padding)
        )

        val hoveredIndex = getHoveredIndex()

        LeapMenu.players.forEachIndexed { i, entry ->
            if (entry == null) return@forEachIndexed

            val (x, y) = offsets[i]
            drawPlayerBox(ctx, x, y, entry.player, i == hoveredIndex)
        }

        pose.popMatrix()
        Resolution.pop(ctx)
    }

    private fun drawPlayerBox(ctx: GuiGraphicsExtractor, x: Float, y: Float, player: DungeonPlayer, isHovered: Boolean) {
        val bgColor = leapBoxColor(player, isHovered)

        ctx.drawFloatingRect(x, y, boxWidth, boxHeight, bgColor.withAlpha(190))

        val headX = (x + 10).toInt()
        val headY = (y + (boxHeight / 2) - headSize / 2).toInt()

        ctx.drawPlayerHead(headX, headY, headSize, player.skin)
        ctx.drawBorder(headX, headY, headSize, headSize, player.clazz.color)

        val textX = (x + 10 + headSize + 5).toInt()
        val textY = (y + boxHeight / 2 - mc.font.lineHeight).toInt()

        ctx.drawString(player.name, textX, textY + 2, player.clazz.color)

        val status = if (player.isDead) "§cDEAD" else player.clazz.name
        ctx.drawString(status, textX, textY + 12, player.clazz.color)
    }

    override fun getHoveredIndex(): Int? {
        val cx = mc.window.screenWidth / 2
        val cy = mc.window.screenHeight / 2
        val mx = mc.mouseHandler.xpos()
        val my = mc.mouseHandler.ypos()

        return when {
            mx < cx && my < cy -> 0
            mx > cx && my < cy -> 1
            mx < cx && my > cy -> 2
            mx > cx && my > cy -> 3
            else -> null
        }
    }
}
