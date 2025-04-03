package noammaddons.utils

import net.minecraft.network.play.server.*
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostPacketEvent
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.removeUnicode
import noammaddons.utils.LocationUtils.inSkyblock

object ScoreboardUtils {
    /**
     * @return A list of player names displayed in the sidebar of the Minecraft scoreboard.
     */
    var sidebarLines: List<String> = emptyList()


    @SubscribeEvent
    fun onScoreboardChange(event: PostPacketEvent.Received) {
        if (! inSkyblock) return
        if (event.packet !is S3EPacketTeams
            && event.packet !is S3CPacketUpdateScore
            && event.packet !is S3DPacketDisplayScoreboard
        ) return

        getSidebarLines()
    }


    private fun getSidebarLines() {
        val objective = mc.theWorld?.scoreboard?.getObjectiveInDisplaySlot(1)
        if (objective == null) {
            sidebarLines = emptyList()
            return
        }
        val title = objective.displayName

        sidebarLines = mc.theWorld.scoreboard.getSortedScores(objective)
            .asSequence()
            .filterNot { it?.playerName?.startsWith("#") == true }
            .take(15)
            .map { ScorePlayerTeam.formatPlayerName(mc.theWorld.scoreboard.getPlayersTeam(it.playerName), it.playerName) }
            .plus(title)
            .toList()
    }


    fun cleanSB(scoreboard: String): String = removeUnicode(scoreboard.removeFormatting())
}
