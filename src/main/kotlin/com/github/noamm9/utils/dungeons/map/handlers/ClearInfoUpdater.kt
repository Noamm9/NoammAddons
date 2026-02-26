package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.impl.dungeon.map.DungeonMap
import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.map.core.RoomData
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.network.ProfileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent

object ClearInfoUpdater {
    private val componentSeparator = createComponent(" &f|&r ")
    private val cdebug get() = NoammAddons.debugFlags.contains("clearinfo")

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
        return@launch // todo api
        if (! DungeonMap.enabled) return@launch
        if (! MapConfig.printPlayersClearInfo.value) return@launch
        DungeonListener.dungeonTeammates.toList().forEach { teammate ->
            val name = teammate.name
            val ci = DungeonPlayer.get(name) ?: return@forEach
            ProfileUtils.getSecrets(name).onSuccess { secrets ->
                ci.secretsBeforeRun = secrets
                if (cdebug) ChatUtils.modMessage("$name has $secrets")
            }.onFailure {
                ci.secretsBeforeRun = 0
                ChatUtils.modMessage("Failed to get secrets for &b$name&r. &cError: ${it.message}")
                it.printStackTrace()
            }
        }
    }

    fun sendClearInfoMessage() = NoammAddons.scope.launch(Dispatchers.IO) {
        if (! DungeonMap.enabled) return@launch
        if (! MapConfig.printPlayersClearInfo.value) return@launch
        val teammates = DungeonListener.dungeonTeammates.toList()

        val msgList = teammates.map { teammate ->
            val before = teammate.secretsBeforeRun
            val secretsAfterRun = if (before != 0L) ProfileUtils.getSecrets(teammate.name).getOrDefault(before) else 0L
            if (cdebug) ChatUtils.modMessage("${teammate.name} has $secretsAfterRun after run")
            val playerFormatted = "${teammate.clazz.code}${teammate.name}"
            val foundSecrets = secretsAfterRun - before

            val baseComp = createComponent("${NoammAddons.PREFIX} $playerFormatted&f:&r ")
            val solo = teammate.clearedRooms.first
            val stacked = teammate.clearedRooms.second
            val deaths = teammate.deaths

            val comps = listOfNotNull(
                getRoomsClearedComponent(solo, stacked),
                //    createComponent("&b$foundSecrets Secrets&r"), todo api
                getDeathCountComponent(deaths)
            )

            constructMessage(baseComp, comps)
        }

        msgList.forEach(ChatUtils::chat)
    }


    private fun constructMessage(baseComp: MutableComponent, allComps: List<MutableComponent>): MutableComponent {
        allComps.forEachIndexed { i, c ->
            baseComp.append(c)
            if (i != allComps.lastIndex) {
                baseComp.append(componentSeparator)
            }
        }
        return baseComp
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

    private fun getDeathCountComponent(deaths: Collection<String>): MutableComponent? {
        return if (deaths.isEmpty()) null
        else createComponent("&c${deaths.size} ${if (deaths.size > 1) "Deaths" else "Death"}", deaths.joinToString("\n"))
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