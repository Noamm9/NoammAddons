package noammaddons.utils

import net.minecraft.scoreboard.ScorePlayerTeam
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.removeUnicode
import noammaddons.utils.ThreadUtils.loop

object ScoreboardUtils {
    /**
     * @return A list of player names displayed in the sidebar of the Minecraft scoreboard.
     */
    var sidebarLines: List<String> = emptyList()

    init {
        loop(250) {
            sidebarLines = mc.theWorld?.scoreboard?.run {
                val objective = getObjectiveInDisplaySlot(1) ?: return@run emptyList()
                val title = objective.displayName

                val lines = getSortedScores(objective)
                    .filter { it?.playerName?.startsWith("#") == false }
                    .let { if (it.size > 15) it.drop(15) else it }
                    .map { ScorePlayerTeam.formatPlayerName(getPlayersTeam(it.playerName), it.playerName) }

                lines + title
            } ?: emptyList()
        }
    }


    fun cleanSB(scoreboard: String): String = removeUnicode(scoreboard.removeFormatting())
}
