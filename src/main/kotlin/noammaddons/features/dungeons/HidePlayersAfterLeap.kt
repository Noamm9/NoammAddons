package noammaddons.features.dungeons

import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.DungeonUtils
import noammaddons.utils.LocationUtils
import noammaddons.utils.ThreadUtils.setTimeout


object HidePlayersAfterLeap: Feature() {
    private val tpRegex = Regex("^You have teleported to .+!$")
    private var HidePlayers = false

    @SubscribeEvent
    fun onLeap(event: Chat) {
        if (! config.hidePlayersAfterLeap) return
        if (! LocationUtils.inDungeon) return
        if (! event.component.noFormatText.matches(tpRegex)) return

        HidePlayers = true
        setTimeout(3500) { HidePlayers = false }
    }

    @SubscribeEvent
    fun onRenderPlayer(event: RenderPlayerEvent.Pre) {
        if (! HidePlayers) return
        if (event.entity == mc.thePlayer) return
        if (DungeonUtils.dungeonTeammates.none { it.entity == event.entity }) return
        if (event.entityPlayer.getDistanceToEntity(mc.thePlayer) > 2) return
        event.isCanceled = true
    }
}

