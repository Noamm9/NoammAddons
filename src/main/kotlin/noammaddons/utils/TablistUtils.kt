package noammaddons.utils

import noammaddons.noammaddons.Companion.mc
import com.google.common.collect.ComparisonChain
import net.minecraft.client.network.NetworkPlayerInfo
import com.google.common.collect.Ordering
import net.minecraft.world.WorldSettings.GameType
import net.minecraft.client.gui.GuiPlayerTabOverlay
import net.minecraft.util.IChatComponent
import java.lang.reflect.Field
import net.minecraft.world.WorldSettings
import noammaddons.mixins.AccessorGuiPlayerTabOverlay
import noammaddons.utils.PlayerUtils.Player


object TablistUtils {
	
    /**
	 * @return The footer text as a string, or null if the footer text is not available.
	 */
	fun getTabListFooterText(): String? {
	    return (mc.ingameGUI.tabList as? AccessorGuiPlayerTabOverlay)?.footer?.formattedText
	}
	
	/**
	 * @return A list of pairs, where each pair contains a [NetworkPlayerInfo] object and a player name as a string.
	 */
	val getTabList: List<Pair<NetworkPlayerInfo, String>>
		get() = (Player?.sendQueue?.playerInfoMap?.sortedWith(Comparator<NetworkPlayerInfo> { o1, o2 ->
			if (o1 == null) return@Comparator -1
			if (o2 == null) return@Comparator 0
			return@Comparator ComparisonChain.start().compareTrueFirst(
				o1.gameType != GameType.SPECTATOR,
				o2.gameType != GameType.SPECTATOR
			).compare(
				o1.playerTeam?.registeredName ?: "",
				o2.playerTeam?.registeredName ?: ""
			).compare(o1.gameProfile.name, o2.gameProfile.name).result()
		}) ?: emptyList())
			.map { Pair(it, mc.ingameGUI.tabList.getPlayerName(it)) }













    internal class PlayerComparator internal constructor() : Comparator<NetworkPlayerInfo> {
        override fun compare(playerOne: NetworkPlayerInfo, playerTwo: NetworkPlayerInfo): Int {
            val teamOne = playerOne.playerTeam
            val teamTwo = playerTwo.playerTeam

            return ComparisonChain
                .start()
                .compareTrueFirst(
                    playerOne.gameType != GameType.SPECTATOR,
                    playerTwo.gameType != GameType.SPECTATOR
                ).compare(
                    teamOne?.registeredName ?: "",
                    teamTwo?.registeredName ?: ""
                ).compare(
                    playerOne.gameProfile.name,
                    playerTwo.gameProfile.name
                ).result()
        }
    }
}