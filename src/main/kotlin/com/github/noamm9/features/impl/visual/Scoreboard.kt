package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.world.scores.*
import java.awt.Color
import net.minecraft.world.scores.Scoreboard as MCScoreboard

object Scoreboard: Feature("Draws a custom scoreboard instead of the vanilla one.") {
    private val hideServerId by ToggleSetting("Hide Server ID").withDescription("Hides the 'm151AM' text from the scoreboard")
    private val backgroundOpacity by SliderSetting("Background Opacity", 75, 0, 100, 1, "%").withDescription("The opacity of the scoreboard background. 0% is transparent, 100% is opaque.")

    private var needsUpdate = true
    private val cachedLines = mutableListOf<String>()
    private var cachedTitle = ""
    private var cachedW = 0f
    private var cachedH = 0f

    @Suppress("RemoveRedundantQualifierName")
    private val hud = object: HudElement() {
        override val name = "Scoreboard"
        override val toggle get() = Scoreboard.enabled

        override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Float, Float> {
            val objective = level.scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return 0f to 0f

            if (needsUpdate) updateCache(level.scoreboard, objective)
            if (cachedLines.isEmpty()) return 0f to 0f

            val boxWidth = cachedW.toDouble()
            val boxHeight = cachedH.toDouble()
            val xOffset = - boxWidth - 5
            val yOffset = - (boxHeight / 2)
            val padding = 8
            val backgroundAlpha = (backgroundOpacity.value / 100f * 255).toInt()

            ctx.drawRect(xOffset, yOffset, boxWidth, boxHeight, Color(15, 15, 15, backgroundAlpha))
            ctx.drawRect(xOffset, yOffset, boxWidth, 2.0, Style.accentColor)
            ctx.drawRect(xOffset - 1, yOffset - 1, boxWidth + 2.0, boxHeight + 2.0, Color.WHITE.withAlpha(backgroundAlpha * 20 / 190))

            ctx.drawCenteredString(cachedTitle, (xOffset + boxWidth / 2).toFloat(), (yOffset + padding).toFloat(), shadow = false)

            val startY = yOffset + padding + mc.font.lineHeight + 4
            val lineHeights = mc.font.lineHeight + 2

            cachedLines.forEachIndexed { index, text ->
                ctx.drawString(text, (xOffset + padding).toFloat(), (startY + (index * lineHeights)).toFloat())
            }

            return cachedW to cachedH
        }

        override fun bounds(): Bounds {
            if (cachedW == 0f) return Bounds(x, y, x, y)
            val left = x - (cachedW + 5) * scale
            val top = y - cachedH * scale / 2f
            return Bounds(left, top, left + cachedW * scale, top + cachedH * scale)
        }

        override fun isHovered(mx: Int, my: Int) = cachedW != 0f && bounds().contains(mx.toFloat(), my.toFloat())

        override fun drawBackground(ctx: GuiGraphicsExtractor, mx: Int, my: Int) {
            if (cachedW == 0f) return
            val bounds = bounds()
            val scaledW = bounds.right - bounds.left
            val scaledH = bounds.bottom - bounds.top

            val borderColor = if (isDragging || isHovered(mx, my)) Style.accentColor else Color(255, 255, 255, 40)

            ctx.drawRect(bounds.left, bounds.top, scaledW, scaledH, Color(10, 10, 10, 150))
            ctx.drawRect(bounds.left, bounds.top, scaledW, 1f, borderColor)
            ctx.drawRect(bounds.left, bounds.bottom - 1f, scaledW, 1f, borderColor)
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

        var maxW = cachedTitle.width().toFloat()

        scores.forEachIndexed { index, score ->
            val name = score.ownerName().string
            val team = scoreboard.getPlayersTeam(name)
            var line = PlayerTeam.formatNameForTeam(team, Component.literal(name)).formattedText

            if (LocationUtils.inSkyblock && hideServerId.value && index == 0) {
                line = "§7Date: " + line.substringBefore(" §8")
            }

            cachedLines.add(line)
            maxW = maxOf(maxW, line.width().toFloat())
        }

        val padding = 8f
        val lineHeight = mc.font.lineHeight
        cachedW = maxW + (padding * 2)
        cachedH = (cachedLines.size * (lineHeight + 2)) + lineHeight + (padding * 2f)
        needsUpdate = false
    }
}