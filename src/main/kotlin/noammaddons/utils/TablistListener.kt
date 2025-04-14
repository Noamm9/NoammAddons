package noammaddons.utils

import gg.essential.elementa.state.BasicState
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.events.WorldUnloadEvent
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.Utils.equalsOneOf

object TablistListener {
    private val deathsRegex = Regex("§r§a§lTeam Deaths: §r§f(?<deaths>\\d+)§r")
    private val cryptsPattern = Regex("§r Crypts: §r§6(?<crypts>\\d+)§r")
    private val secretsFoundPattern = Regex("§r Secrets Found: §r§b(?<secrets>\\d+)§r")
    private val timeElapsedPattern = Regex("Time Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")
    private val complatedRoomsRegex = Regex("^§r Completed Rooms: §r§d(\\d+)§r\$")
    private val presentClearRegex = Regex("^Cleared: §[c6a](\\d+)% §8(?:§8)?\\(\\d+\\)\$")

    var timeElapsed = - 1
    var secretsFound = 0
    val cryptsCount = BasicState(0)
    var deathCount = 0
    var secretTotal = 0
    val deathPenalty get() = (deathCount * 2 - 1).coerceAtLeast(0)

    var percentCleared = 0.0
    var completedRooms = 0


    @SubscribeEvent
    fun onWorldUnload(e: WorldUnloadEvent) = reset()

    fun reset() {
        secretsFound = 0
        timeElapsed = - 1
        cryptsCount.set(0)
        deathCount = 0
        secretTotal = 0
        percentCleared = 0.0
        completedRooms = 0
    }

    @SubscribeEvent
    fun onScoreboardChange(event: PacketEvent.Received) {
        if (! inDungeon || event.packet !is S3EPacketTeams) return
        if (event.packet.action != 2) return
        val line = event.packet.players.joinToString(
            " ",
            prefix = event.packet.prefix,
            postfix = event.packet.suffix
        ).removeFormatting()

        if (line.startsWith("Time Elapsed:")) {
            val match = timeElapsedPattern.matchEntire(line)?.groups ?: return
            val hours = match["hrs"]?.value?.toIntOrNull() ?: 0
            val minutes = match["min"]?.value?.toIntOrNull() ?: 0
            val seconds = match["sec"]?.value?.toIntOrNull() ?: 0
            timeElapsed = hours * 3600 + minutes * 60 + seconds
        }
        else if (line.startsWith("Cleared:")) {
            percentCleared = presentClearRegex.firstResult(line)?.toDoubleOrNull() ?: percentCleared
        }
    }

    @SubscribeEvent
    fun onTabList(event: PacketEvent.Received) {
        if (! inDungeon) return
        if (event.packet !is S38PacketPlayerListItem) return
        if (! event.packet.action.equalsOneOf( // @formatter:off
           S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME,
           S38PacketPlayerListItem.Action.ADD_PLAYER
        )) return

        event.packet.entries.forEach {
            val text = it?.displayName?.formattedText ?: it?.profile?.name ?: return@forEach
            updateFromTabList(text)
        }
    }

    private fun updateFromTabList(text: String) {
        when {
            text.contains("Secrets Found:") -> if (!text.contains("%")) secretsFound = secretsFoundPattern.firstResult(text)?.toIntOrNull() ?: secretsFound
            text.contains("Crypts:") -> cryptsCount.set(cryptsPattern.firstResult(text)?.toIntOrNull() ?: cryptsCount.get())
            text.contains("Completed Rooms:") -> completedRooms = complatedRoomsRegex.firstResult(text)?.toIntOrNull() ?: completedRooms
            text.contains("Team Deaths:") -> deathCount = deathsRegex.firstResult(text)?.toIntOrNull() ?: deathCount
            else -> {}
        }
    }


    private fun Regex.firstResult(input: CharSequence): String? {
        return this.matchEntire(input)?.groups?.get(1)?.value
    }
}