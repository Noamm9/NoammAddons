package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam
import java.awt.Color
import net.minecraft.world.scores.Scoreboard as MCScoreboard

object Scoreboard: Feature("Draws a custom scoreboard instead of the vanilla one.") {
    private val hideServerId by ToggleSetting("Hide Server ID").withDescription("Hides the 'm151AM' text from the scoreboard")

    private var needsUpdate = true
    private val cachedLines = mutableListOf<String>()
    private var cachedTitle = ""
    private var cachedW = 0f
    private var cachedH = 0f

    @Suppress("RemoveRedundantQualifierName")
    private val hud = object: HudElement() {
        override val name = "Scoreboard"
        override val toggle get() = Scoreboard.enabled

        override fun draw(ctx: GuiGraphics, example: Boolean): Pair<Float, Float> {
            val scoreboard = mc.level?.scoreboard ?: return 0f to 0f
            val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return 0f to 0f

            if (needsUpdate) updateCache(scoreboard, objective)
            if (cachedLines.isEmpty()) return 0f to 0f

            val boxWidth = cachedW.toDouble()
            val boxHeight = cachedH.toDouble()
            val xOffset = - boxWidth - 5
            val yOffset = - (boxHeight / 2)
            val padding = 8

            Render2D.drawRect(ctx, xOffset, yOffset, boxWidth, boxHeight, Color(15, 15, 15, 190))
            Render2D.drawRect(ctx, xOffset, yOffset, boxWidth, 2.0, Style.accentColor)
            Render2D.drawRect(ctx, xOffset - 1, yOffset - 1, boxWidth + 2.0, boxHeight + 2.0, Color(255, 255, 255, 20))

            Render2D.drawCenteredString(ctx, cachedTitle, (xOffset + boxWidth / 2).toFloat(), (yOffset + padding).toFloat(), shadow = false)

            val startY = yOffset + padding + mc.font.lineHeight + 4
            val lineHeights = mc.font.lineHeight + 2

            cachedLines.forEachIndexed { index, text ->
                Render2D.drawString(ctx, text, (xOffset + padding).toFloat(), (startY + (index * lineHeights)).toFloat())
            }

            return cachedW to cachedH
        }

        override fun isHovered(mx: Int, my: Int): Boolean {
            if (cachedW == 0f) return false
            val visualWidth = cachedW * scale
            val visualHeight = cachedH * scale
            return mx >= x - visualWidth - 5 && mx <= x && my >= y - (visualHeight / 2) && my <= y + (visualHeight / 2)
        }

        override fun drawBackground(ctx: GuiGraphics, mx: Int, my: Int) {
            if (cachedW == 0f) return
            val scaledW = cachedW * scale
            val scaledH = cachedH * scale
            val drawX = x - scaledW - 5
            val drawY = y - (scaledH / 2)

            val hovered = mx >= drawX && mx <= drawX + scaledW && my >= drawY && my <= drawY + scaledH
            val borderColor = if (isDragging || hovered) Style.accentColor else Color(255, 255, 255, 40)

            Render2D.drawRect(ctx, drawX.toDouble(), drawY.toDouble(), scaledW.toDouble(), scaledH.toDouble(), Color(10, 10, 10, 150))
            Render2D.drawRect(ctx, drawX.toDouble(), drawY.toDouble(), scaledW.toDouble(), 1.0, borderColor)
            Render2D.drawRect(ctx, drawX.toDouble(), (drawY + scaledH - 1).toDouble(), scaledW.toDouble(), 1.0, borderColor)
        }
    }

    override fun init() {
        hud.x = 200f
        hud.y = 200f
        hudElements.add(hud)

        register<MainThreadPacketReceivedEvent.Post> {
            if (
                event.packet is ClientboundSetScorePacket || event.packet is ClientboundSetObjectivePacket ||
                event.packet is ClientboundSetDisplayObjectivePacket || event.packet is ClientboundResetScorePacket ||
                event.packet is ClientboundSetPlayerTeamPacket
            ) {
                needsUpdate = true
            }
        }
    }

    private fun updateCache(scoreboard: MCScoreboard, objective: Objective) {
        cachedLines.clear()
        cachedTitle = objective.displayName.formattedText

        val scores = scoreboard.listPlayerScores(objective).sortedByDescending { it.value }.take(15)

        if (scores.isEmpty()) {
            cachedW = 0f
            cachedH = 0f
            needsUpdate = false
            return
        }

        val font = mc.font
        var maxW = font.width(cachedTitle).toFloat()

        scores.forEachIndexed { index, score ->
            val name = score.ownerName().string
            val team = scoreboard.getPlayersTeam(name)
            var line = PlayerTeam.formatNameForTeam(team, Component.literal(name)).formattedText

            if (LocationUtils.inSkyblock && hideServerId.value && index == 0) {
                line = "§7Date: " + line.substringBefore(" §8")
            }

            cachedLines.add(line)
            maxW = maxOf(maxW, font.width(line).toFloat())
        }

        val padding = 8f
        val lineHeights = font.lineHeight + 2
        cachedW = maxW + (padding * 2)
        cachedH = (cachedLines.size * lineHeights) + font.lineHeight + (padding * 2f)
        needsUpdate = false
    }
}