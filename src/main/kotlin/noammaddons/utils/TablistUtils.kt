package noammaddons.utils

import com.google.common.collect.ComparisonChain
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.world.WorldSettings.*
import noammaddons.mixins.AccessorGuiPlayerTabOverlay
import noammaddons.NoammAddons.Companion.mc


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
        get() = (mc.thePlayer?.sendQueue?.playerInfoMap?.sortedWith(Comparator<NetworkPlayerInfo> { o1, o2 ->
            if (o1 == null) return@Comparator - 1
            if (o2 == null) return@Comparator 0
            return@Comparator ComparisonChain.start().compareTrueFirst(
                o1.gameType != GameType.SPECTATOR,
                o2.gameType != GameType.SPECTATOR
            ).compare(
                o1.playerTeam?.registeredName ?: "",
                o2.playerTeam?.registeredName ?: ""
            ).compare(o1.gameProfile.name, o2.gameProfile.name).result()
        }) ?: emptyList()).map { Pair(it, mc.ingameGUI.tabList.getPlayerName(it)) }
}