package com.github.noamm9.features.impl.dungeon.leapmenu

import com.github.noamm9.features.impl.dungeon.map.MapRenderer
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

object MapLeapRenderer: LeapMenuRenderer() {
    private var hoveredIndex: Int? = null

    override fun render(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        Resolution.push(ctx)

        val sw = Resolution.width
        val sh = Resolution.height
        val mx = Resolution.getMouseX(mouseX).toFloat()
        val my = Resolution.getMouseY(mouseY).toFloat()
        val mapSize = 128f

        val scale = LeapMenu.mapLeapScale.value
        val mapPix = mapSize * scale

        val mapOx = (sw - mapPix) / 2f
        val mapOy = (sh - mapPix - (8 * scale)) / 2f

        fun playerDot(player: DungeonPlayer): Triple<Float, Float, Float> {
            val (pmx, pmz) = teammateMapPos(player)
            return Triple(mapOx + pmx * scale, mapOy + pmz * scale, 7f * scale)
        }

        hoveredIndex = null

        LeapMenu.players.forEachIndexed { i, entry ->
            if (entry == null || entry.player.isDead) return@forEachIndexed
            val (px, py, r) = playerDot(entry.player)
            if (mx in px - r .. px + r && my in py - r .. py + r) hoveredIndex = i
        }

        ctx.drawRect(0, 0, sw, sh, Color.BLACK.withAlpha(170))

        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(mapOx, mapOy)
        pose.scale(scale)
        MapRenderer.draw(ctx, example = false, extraInfo = false)
        pose.popMatrix()

        hoveredIndex?.let { idx ->
            LeapMenu.players[idx]?.let { entry ->
                val (px, py, r) = playerDot(entry.player)
                ctx.drawBorder(px - r, py - r, r * 2f, r * 2f, thickness = 2)
            }
        }

        Resolution.pop(ctx)
    }

    override fun getHoveredIndex() = hoveredIndex

    private fun teammateMapPos(player: DungeonPlayer): Pair<Float, Float> {
        val entity = player.entity
        return if (entity == null || ! entity.isAlive) player.mapX to player.mapZ
        else MapUtils.coordsToMap(entity.renderVec)
    }
}
