package noammaddons.utils

import net.minecraft.scoreboard.ScorePlayerTeam
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.removeUnicode

object ScoreboardUtils {
	fun cleanSB(scoreboard: String): String = removeUnicode(scoreboard.removeFormatting())

	/**
	 * @return A list of player names displayed in the sidebar of the Minecraft scoreboard.
	 */
	val sidebarLines: List<String>
	    get() = mc.theWorld?.scoreboard?.run {
	        val objective = getObjectiveInDisplaySlot(1) ?: return emptyList()
	        getSortedScores(objective)
	            .filter { it?.playerName?.startsWith("#") == false }
	            .let { if (it.size > 15) it.drop(15) else it }
	            .map { ScorePlayerTeam.formatPlayerName(getPlayersTeam(it.playerName), it.playerName) }
	    } ?: emptyList()
}
