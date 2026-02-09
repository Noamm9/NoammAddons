package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.PacketEvent
import com.google.common.collect.ComparisonChain
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.level.GameType

object TabListUtils {
    private var cachedLines: List<Pair<Component, PlayerInfo>> = emptyList()
    private var listDirty = true

    init {
        EventBus.register<PacketEvent.Received>(EventPriority.HIGH) {
            if (event.packet is ClientboundPlayerInfoUpdatePacket) {
                listDirty = true
            }
        }
    }

    fun getTabList(): List<Pair<Component, PlayerInfo>> {
        if (listDirty) {
            cachedLines = fetchTabList()
            listDirty = false
        }
        return cachedLines
    }

    private fun fetchTabList(): List<Pair<Component, PlayerInfo>> {
        val player = mc.player ?: return emptyList()

        val onlinePlayers = player.connection.onlinePlayers
        val sortedPlayers = onlinePlayers.sortedWith(PlayerComparator)

        val result = mutableListOf<Pair<Component, PlayerInfo>>()

        for (info in sortedPlayers) {
            val component = mc.gui.tabList.getNameForDisplay(info)
            result.add(component to info)
        }

        return if (result.size > 80) result.subList(0, 80) else result
    }

    private object PlayerComparator: Comparator<PlayerInfo> {
        override fun compare(o1: PlayerInfo, o2: PlayerInfo): Int {
            val team1 = o1.team
            val team2 = o2.team

            return ComparisonChain.start()
                .compareTrueFirst(o1.gameMode != GameType.SPECTATOR, o2.gameMode != GameType.SPECTATOR)
                .compare(
                    if (team1 != null) team1.name else "",
                    if (team2 != null) team2.name else ""
                )
                .compare(o1.profile.name, o2.profile.name)
                .result()
        }
    }
}