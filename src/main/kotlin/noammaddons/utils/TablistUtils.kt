package noammaddons.utils

import com.google.common.collect.ComparisonChain
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.world.WorldSettings.GameType
import noammaddons.NoammAddons.Companion.mc
import noammaddons.mixins.accessor.AccessorGuiPlayerTabOverlay


object TablistUtils {
    fun getTabListFooterText(): String? {
        return (mc.ingameGUI.tabList as? AccessorGuiPlayerTabOverlay)?.footer?.formattedText
    }

    val tabList: List<Pair<NetworkPlayerInfo, String>>
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


    fun getDungeonTabList(): List<Pair<NetworkPlayerInfo, String>>? {
        return tabList.let { if (it.size > 18 && it[0].second.contains("§r§b§lParty §r§f(")) it else null }
    }
}