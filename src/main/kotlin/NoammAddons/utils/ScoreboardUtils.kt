package NoammAddons.utils

import net.minecraft.scoreboard.ScorePlayerTeam
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.removeFormatting

object ScoreboardUtils {
	fun cleanSB(scoreboard: String): String = scoreboard.removeFormatting().filter { it.code in 32..126 }

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
