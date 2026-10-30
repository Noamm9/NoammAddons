package com.github.noamm9.features.impl.dungeon.leapmenu

import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.render.Render2D.drawAnnularSegment
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawLine
import com.github.noamm9.utils.render.Render2D.drawPlayerHead
import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.*

object WheelLeapRenderer: LeapMenuRenderer() {
    private const val SLOTS = 5
    private const val SEGMENT_ANGLE = PI * 2.0 / SLOTS
    private const val OUTER_RADIUS = 130f
    private const val INNER_RADIUS = OUTER_RADIUS * 0.45f
    private const val DEAD_ZONE = 5f
    private const val HEAD_SIZE = 32

    private val separatorColor = Color(255, 255, 255, 40)

    private var hoveredIndex: Int? = null

    override fun render(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        Resolution.push(ctx)
        val userScale = (LeapMenu.scale.value.toFloat() / 100f) * 2.0f
        val centerX = Resolution.width / userScale / 2f
        val centerY = Resolution.height / userScale / 2f
        val mx = Resolution.getMouseX(mouseX) / userScale
        val my = Resolution.getMouseY(mouseY) / userScale

        ctx.drawRect(0, 0, Resolution.width, Resolution.height, Color.BLACK.withAlpha(150))

        val pose = ctx.pose()
        pose.pushMatrix()
        pose.scale(userScale)

        hoveredIndex = null
        val hoveredSlot = hoveredSlot(mx - centerX, my - centerY)
        var hoveredPlayer: DungeonPlayer? = null

        wheelSlots().forEachIndexed { i, player ->
            if (player == null) return@forEachIndexed
            val isSelf = player == DungeonListener.thePlayer
            val playerIndex = LeapMenu.players.indexOfFirst { it?.player == player }
            if (! isSelf && playerIndex == - 1) return@forEachIndexed

            val isHovered = ! isSelf && i == hoveredSlot
            if (isHovered) {
                hoveredIndex = playerIndex
                hoveredPlayer = player
            }

            val color = leapBoxColor(player, isHovered)
            val angle = - PI / 2.0 + i * SEGMENT_ANGLE
            ctx.drawAnnularSegment(centerX, centerY, INNER_RADIUS, OUTER_RADIUS, angle - SEGMENT_ANGLE / 2.0, angle + SEGMENT_ANGLE / 2.0, color.withAlpha(190))

            drawPlayerInSegment(ctx, player, angle, centerX, centerY, isSelf)
        }

        repeat(SLOTS) { i ->
            val angle = - PI / 2.0 - SEGMENT_ANGLE / 2.0 + i * SEGMENT_ANGLE
            val cosAngle = cos(angle).toFloat()
            val sinAngle = sin(angle).toFloat()
            ctx.drawLine(
                centerX + cosAngle * (INNER_RADIUS - 1f), centerY + sinAngle * (INNER_RADIUS - 1f),
                centerX + cosAngle * (OUTER_RADIUS + 1f), centerY + sinAngle * (OUTER_RADIUS + 1f),
                separatorColor, 2f
            )
        }

        hoveredPlayer?.let { player ->
            ctx.drawCenteredString(player.name, centerX, centerY - 9f, player.clazz.color)
            val status = if (player.isDead) "§cDEAD" else player.clazz.name
            ctx.drawCenteredString(status, centerX, centerY + 2f, player.clazz.color, scale = 0.8f)
        }

        pose.popMatrix()
        Resolution.pop(ctx)
    }

    private fun wheelSlots(): Array<DungeonPlayer?> {
        val slots = arrayOfNulls<DungeonPlayer>(SLOTS)
        val team = DungeonListener.dungeonTeammates.toList()

        if (LeapMenu.sorting.value != 0 && LeapMenu.sorting.value != 2) {
            LeapMenu.sortTeammates(team).take(SLOTS).forEachIndexed { i, player -> slots[i] = player }
            return slots
        }

        val leftovers = mutableListOf<DungeonPlayer>()
        for (player in team.sortedBy { it.name }) {
            val index = player.clazz.ordinal
            if (index < SLOTS && slots[index] == null) slots[index] = player
            else leftovers.add(player)
        }

        for (player in leftovers) {
            val free = slots.indexOfFirst { it == null }.takeIf { it != - 1 } ?: break
            slots[free] = player
        }

        return slots
    }

    private fun drawPlayerInSegment(ctx: GuiGraphicsExtractor, player: DungeonPlayer, angle: Double, centerX: Float, centerY: Float, isSelf: Boolean) {
        val radius = (INNER_RADIUS + OUTER_RADIUS) / 2f
        val headX = (centerX + cos(angle).toFloat() * radius - HEAD_SIZE / 2f).toInt()
        val headY = (centerY + sin(angle).toFloat() * radius - HEAD_SIZE / 2f - 5f).toInt()

        ctx.drawPlayerHead(headX, headY, HEAD_SIZE, player.skin)
        ctx.drawBorder(headX, headY, HEAD_SIZE, HEAD_SIZE, player.clazz.color)

        val label = if (isSelf) "§7You" else player.name
        ctx.drawCenteredString(label, headX + HEAD_SIZE / 2f, headY + HEAD_SIZE + 3f, player.clazz.color, scale = 0.75f)
    }

    private fun hoveredSlot(x: Float, y: Float): Int? {
        if (hypot(x, y) <= DEAD_ZONE) return null
        return Math.floorMod(floor((atan2(y, x) + PI / 2.0 + SEGMENT_ANGLE / 2.0) / SEGMENT_ANGLE).toInt(), SLOTS)
    }

    override fun getHoveredIndex() = hoveredIndex

    fun getKeybindIndex(slot: Int): Int? {
        val player = wheelSlots().filterNotNull().filter { it != DungeonListener.thePlayer }.getOrNull(slot) ?: return null
        return LeapMenu.players.indexOfFirst { it?.player == player }.takeIf { it != - 1 }
    }
}
