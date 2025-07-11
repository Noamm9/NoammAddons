package noammaddons.utils

import net.minecraft.network.play.server.*
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.PostPacketEvent
import noammaddons.events.WorldUnloadEvent
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.removeUnicode

object ScoreboardUtils {
    var sidebarLines: List<String> = emptyList()
        private set

    @SubscribeEvent
    fun onScoreboardChange(event: PostPacketEvent.Received) {
        if (event.packet !is S3EPacketTeams
            && event.packet !is S3CPacketUpdateScore
            && event.packet !is S3DPacketDisplayScoreboard
        ) return

        getSidebarLines()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        sidebarLines = emptyList()
    }


    private fun getSidebarLines() {
        val objective = mc.theWorld?.scoreboard?.getObjectiveInDisplaySlot(1)
        if (objective == null) {
            sidebarLines = emptyList()
            return
        }

        sidebarLines = mc.theWorld.scoreboard.getSortedScores(objective).asSequence()
            .filterNot { it?.playerName?.startsWith("#") == true }.take(15)
            .map { ScorePlayerTeam.formatPlayerName(mc.theWorld.scoreboard.getPlayersTeam(it.playerName), it.playerName) }
            .plus(objective.displayName).toList()
    }

    fun cleanSB(scoreboard: String): String = removeUnicode(scoreboard.removeFormatting())
}
