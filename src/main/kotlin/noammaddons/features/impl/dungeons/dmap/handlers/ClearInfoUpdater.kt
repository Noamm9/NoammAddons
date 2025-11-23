package noammaddons.features.impl.dungeons.dmap.handlers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import noammaddons.NoammAddons.Companion.CHAT_PREFIX
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.scope
import noammaddons.features.impl.dungeons.dmap.DungeonMap.debug
import noammaddons.features.impl.dungeons.dmap.core.ClearInfo
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.DungeonUtils
import noammaddons.utils.ProfileUtils
import noammaddons.utils.Utils.equalsOneOf

object ClearInfoUpdater {
    private val componentSepetator = createComponent(" &f|&r ")

    fun checkSplits(room: RoomData, oldState: RoomState, newState: RoomState, players: List<DungeonUtils.DungeonPlayer>) {
        if (! DungeonMapConfig.printPlayersClearInfo.value) return
        if (players.isEmpty()) return
        if (room.type.equalsOneOf(RoomType.FAIRY, RoomType.ENTRANCE)) return
        if (! oldState.equalsOneOf(RoomState.UNDISCOVERED, RoomState.DISCOVERED, RoomState.UNOPENED)) return
        if (! newState.equalsOneOf(RoomState.CLEARED, RoomState.GREEN)) return

        if (players.size == 1) {
            ClearInfo.get(players[0].name)?.clearedRooms?.first?.add(room.name)
            if (debug) modMessage("${players[0].name} cleared ${room.name}")
        }
        else players.forEach {
            ClearInfo.get(it.name)?.clearedRooms?.second?.add(room.name)
            if (debug) modMessage("${it.name} stacked cleard ${room.name}")
        }
    }

    fun updateDeaths(player: String, reason: String) {
        if (! DungeonMapConfig.printPlayersClearInfo.value) return
        ClearInfo.get(player)?.deaths?.add(reason)
        if (debug) modMessage("$player died: $reason")
    }

    fun initStartSecrets() = scope.launch(Dispatchers.IO) {
        if (! DungeonMapConfig.printPlayersClearInfo.value) return@launch
        DungeonUtils.runPlayersNames.keys.toList().forEach { name ->
            val ci = ClearInfo.get(name) ?: return@forEach
            val secrets = ProfileUtils.getSecrets(name)
            ci.secretsBeforeRun = secrets
        }
    }

    fun sendClearInfoMessage() = scope.launch(Dispatchers.IO) {
        if (! DungeonMapConfig.printPlayersClearInfo.value) return@launch
        val msgList = DungeonUtils.dungeonTeammates.toList().map { teammate ->
            val secretsAfterRun = if (teammate.clearInfo.secretsBeforeRun != 0) ProfileUtils.getSecrets(teammate.name) else 0
            val playerFormatted = "${DungeonUtils.Classes.getColorCode(teammate.clazz)}${teammate.name}"
            val foundSecrets = secretsAfterRun - teammate.clearInfo.secretsBeforeRun
            val baseComp = createComponent("$CHAT_PREFIX $playerFormatted&f:&r ")
            val solo = teammate.clearInfo.clearedRooms.first
            val stacked = teammate.clearInfo.clearedRooms.second
            val deaths = teammate.clearInfo.deaths

            val comps = listOfNotNull(
                getRoomsClearedComponent(solo, stacked),
                createComponent("&b$foundSecrets Secrets&r"),
                getDeathCountComponent(deaths)
            )

            constractMessage(baseComp, comps)
        }

        msgList.forEach(mc.thePlayer::addChatMessage)
    }


    private fun constractMessage(baseComp: ChatComponentText, allComps: List<ChatComponentText>): ChatComponentText {
        allComps.forEachIndexed { i, c ->
            baseComp.appendSibling(c)
            if (i != allComps.lastIndex) {
                baseComp.appendSibling(componentSepetator)
            }
        }
        return baseComp
    }

    private fun getRoomsClearedComponent(solo: Collection<String>, stacked: Collection<String>): ChatComponentText {
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

    private fun getDeathCountComponent(deaths: Collection<String>): ChatComponentText? {
        return if (deaths.isEmpty()) null
        else createComponent("&c${deaths.size} ${if (deaths.size > 1) "Deaths" else "Death"}", deaths.joinToString("\n"))
    }

    private fun createComponent(text: String, hoverText: String? = null) = ChatComponentText(text.addColor()).apply {
        if (hoverText != null) {
            chatStyle = ChatStyle().apply {
                chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText.addColor()))
            }
        }
    }
}
