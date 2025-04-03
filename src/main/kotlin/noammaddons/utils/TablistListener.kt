package noammaddons.utils

import gg.essential.elementa.state.BasicState
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.events.WorldUnloadEvent
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.Utils.equalsOneOf

object TablistListener {
    private val deathsRegex = Regex("§r§a§lTeam Deaths: §r§f(?<deaths>\\d+)§r")
    private val cryptsPattern = Regex("§r Crypts: §r§6(?<crypts>\\d+)§r")
    private val secretsFoundPattern = Regex("§r Secrets Found: §r§b(?<secrets>\\d+)§r")
    private val secretsFoundPercentagePattern = Regex("§r Secrets Found: §r§[ae](?<percentage>[\\d.]+)%§r")
    private val timeElapsedPattern = Regex("Time Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")

    var secretPercentage = 0f
    var secretsFound = - 1
    var timeElapsed = - 1
    var cryptsCount = BasicState(- 1)
    var deathCount = - 1

    val secretTotal get() = (secretsFound / (secretPercentage + 0.0001f) + 0.5).toInt()
    val deathPenalty get() = (deathCount * 2 - 1).coerceAtLeast(0)


    @SubscribeEvent
    fun reset(e: WorldUnloadEvent) {
        deathCount = - 1
        cryptsCount.set(- 1)
        secretsFound = - 1
        timeElapsed = - 1
    }


    @SubscribeEvent
    fun onScoreboardChange(event: PacketEvent.Received) {
        if (! inSkyblock || event.packet !is S3EPacketTeams) return
        if (! inDungeon) return
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
            text.contains("Team Deaths:") -> deathCount = deathsRegex.firstResult(text)?.toIntOrNull() ?: deathCount
            text.contains("Crypts:") -> cryptsCount.set(cryptsPattern.firstResult(text)?.toIntOrNull() ?: cryptsCount.get())
            text.contains("Secrets Found:") -> if (text.contains("%")) secretPercentage = secretsFoundPercentagePattern.firstResult(text)?.toFloatOrNull()?.div(100f) ?: secretPercentage
            else secretsFound = secretsFoundPattern.firstResult(text)?.toIntOrNull() ?: secretsFound
        }
    }

    private fun Regex.firstResult(input: CharSequence): String? {
        return this.matchEntire(input)?.groups?.get(1)?.value
    }
}