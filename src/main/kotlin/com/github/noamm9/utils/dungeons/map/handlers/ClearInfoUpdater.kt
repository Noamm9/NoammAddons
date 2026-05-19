package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.impl.dungeon.map.DungeonMap
import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.map.core.RoomData
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.network.ProfileUtils
import kotlinx.coroutines.*
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

object ClearInfoUpdater {
    private val componentSeparator = createComponent(" &f|&r ")

    fun checkSplits(room: RoomData, oldState: RoomState, newState: RoomState, players: List<DungeonPlayer>) {
        if (! DungeonMap.enabled) return
        if (! MapConfig.printPlayersClearInfo.value) return
        if (players.isEmpty()) return
        if (room.type.equalsOneOf(RoomType.FAIRY, RoomType.ENTRANCE)) return
        if (! oldState.equalsOneOf(RoomState.UNDISCOVERED, RoomState.DISCOVERED, RoomState.UNOPENED)) return
        if (! newState.equalsOneOf(RoomState.CLEARED, RoomState.GREEN)) return

        if (players.size == 1) DungeonPlayer.get(players[0].name)?.clearedRooms?.first?.add(room.name)
        else players.forEach { DungeonPlayer.get(it.name)?.clearedRooms?.second?.add(room.name) }
    }

    fun updateDeaths(player: String, reason: String) {
        if (! DungeonMap.enabled) return
        if (! MapConfig.printPlayersClearInfo.value) return
        DungeonPlayer.get(player)?.deaths?.add(reason)
    }

    fun initStartSecrets() = NoammAddons.scope.launch(Dispatchers.IO) {
        if (! DungeonMap.enabled) return@launch
        if (! MapConfig.printPlayersClearInfo.value) return@launch
        coroutineScope {
            val jobs = DungeonListener.dungeonTeammates.map { teammate ->
                async {
                    ProfileUtils.getSecrets(teammate.name).onSuccess {
                        teammate.secretsBeforeRun = it
                    }.onFailure {
                        teammate.secretsBeforeRun = 0
                        ChatUtils.modMessage("Failed to get secrets for &b${teammate.name}&r. &cError: ${it.message}")
                        it.printStackTrace()
                    }
                }
            }

            jobs.awaitAll()
        }
    }

    fun sendClearInfoMessage() = NoammAddons.scope.launch(Dispatchers.IO) {
        if (! DungeonMap.enabled) return@launch
        if (! MapConfig.printPlayersClearInfo.value) return@launch
        val msgList = DungeonListener.dungeonTeammates.map { teammate ->
            async {
                val before = teammate.secretsBeforeRun
                val secretsAfterRun = if (before != 0L) ProfileUtils.getSecrets(teammate.name).getOrDefault(before) else 0L
                val playerFormatted = "${teammate.clazz.code}${teammate.name}"
                val foundSecrets = secretsAfterRun - before

                val baseComp = createComponent("${NoammAddons.PREFIX} $playerFormatted&f:&r ")
                val (solo, stacked) = teammate.clearedRooms
                val deaths = teammate.deaths

                val comps = listOfNotNull(
                    getRoomsClearedComponent(solo, stacked),
                    createComponent("&b$foundSecrets Secrets&r"),
                    if (deaths.isEmpty()) null else createComponent("&c${deaths.size} ${if (deaths.size > 1) "Deaths" else "Death"}", deaths.joinToString("\n")),
                )

                comps.forEachIndexed { i, c ->
                    baseComp.append(c)
                    if (i != comps.lastIndex) {
                        baseComp.append(componentSeparator)
                    }
                }

                baseComp
            }
        }

        msgList.awaitAll().forEach(ChatUtils::chat)
    }

    private fun getRoomsClearedComponent(solo: Collection<String>, stacked: Collection<String>): MutableComponent {
        return if (solo.size + stacked.size == 0) createComponent("&e0 Rooms&r")
        else {
            val roomRange = if (stacked.isEmpty()) "${solo.size}" else "${solo.size}-${solo.size + stacked.size}"
            val tooltip = buildString {
                append(solo.joinToString("\n") { "$it &b(Solo)&r" })
                if (solo.isNotEmpty() && stacked.isNotEmpty()) append("\n")
                append(stacked.joinToString("\n") { "$it &d(stack)&r" })
            }

            createComponent("&e$roomRange Rooms&r", tooltip)
        }
    }

    private fun createComponent(text: String, hoverText: String? = null): MutableComponent {
        val comp = Component.literal(text.addColor())

        if (hoverText != null) {
            return comp.withStyle { s ->
                s.withHoverEvent(HoverEvent.ShowText(Component.literal(hoverText.addColor())))
            }
        }
        return comp
    }
}