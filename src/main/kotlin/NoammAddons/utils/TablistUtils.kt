package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import com.google.common.collect.ComparisonChain
import net.minecraft.client.network.NetworkPlayerInfo
import com.google.common.collect.Ordering
import net.minecraft.world.WorldSettings.GameType
import net.minecraft.client.gui.GuiPlayerTabOverlay
import net.minecraft.util.IChatComponent
import java.lang.reflect.Field
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.world.WorldSettings


object TablistUtils {

    fun getTablistText(): List<String> {
        if (mc.theWorld == null || mc.ingameGUI.tabList == null) return emptyList()

        return Ordering.from(PlayerComparator()).sortedCopy(mc.thePlayer!!.sendQueue.playerInfoMap).map(mc.ingameGUI.tabList!!::getPlayerName)
    }

    fun getTabListFooterText(): String? {
        val tabOverlay: GuiPlayerTabOverlay = mc.ingameGUI.tabList

        try {
            val footerField: Field = GuiPlayerTabOverlay::class.java.getDeclaredField("footer")
            footerField.isAccessible = true

            val footer: IChatComponent? = footerField.get(tabOverlay) as IChatComponent?

            return footer?.unformattedText

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    val getTabList: List<Pair<NetworkPlayerInfo, String>>
        get() = (mc.thePlayer?.sendQueue?.playerInfoMap?.sortedWith(tabListOrder) ?: emptyList())
            .map { Pair(it, mc.ingameGUI.tabList.getPlayerName(it)) }

    val tabListOrder = Comparator<NetworkPlayerInfo> { o1, o2 ->
        if (o1 == null) return@Comparator -1
        if (o2 == null) return@Comparator 0
        return@Comparator ComparisonChain.start().compareTrueFirst(
            o1.gameType != WorldSettings.GameType.SPECTATOR,
            o2.gameType != WorldSettings.GameType.SPECTATOR
        ).compare(
            o1.playerTeam?.registeredName ?: "",
            o2.playerTeam?.registeredName ?: ""
        ).compare(o1.gameProfile.name, o2.gameProfile.name).result()
    }











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