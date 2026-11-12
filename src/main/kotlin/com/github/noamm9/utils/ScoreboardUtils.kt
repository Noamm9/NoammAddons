package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.init.types.ISelfInit
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

object ScoreboardUtils: ISelfInit {
    private var cachedLines: List<Component> = emptyList()
    private var listDirty = true

    override fun init() {
        EventBus.register<MainThreadPacketReceivedEvent.Post>(EventPriority.HIGHEST) {
            when (event.packet) {
                is ClientboundSetScorePacket, is ClientboundSetObjectivePacket,
                is ClientboundSetDisplayObjectivePacket, is ClientboundResetScorePacket,
                is ClientboundSetPlayerTeamPacket -> listDirty = true
            }
        }
    }

    fun getLines(): List<Component> {
        if (listDirty) {
            cachedLines = fetchScoreboard()
            listDirty = false
        }
        return cachedLines
    }

    private fun fetchScoreboard(): List<Component> {
        val scoreboard = NoammAddons.mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()

        val lines = mutableListOf(objective.displayName)
        scoreboard.listPlayerScores(objective).sortedByDescending { it.value }.take(15).forEach { score ->
            val name = score.ownerName().string
            lines.add(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(name), Component.literal(name)).copy())
        }

        return lines
    }
}