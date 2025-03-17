package noammaddons.features.dungeons

import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ThreadUtils.setTimeout


// Inspired by Hideshichas's CT module: Hideout
// https://github.com/Hideshichan/Hideout/blob/main/Hideout/Features/LeapHelper.js
object HidePlayersAfterLeap: Feature() {
    private var HidePlayers = false

    @SubscribeEvent
    fun onLeap(event: Chat) {
        if (! config.hidePlayersAfterLeap) return
        if (! event.component.noFormatText.matches(Regex("^You have teleported to .+!$"))) return

        HidePlayers = true
        setTimeout(3500) { HidePlayers = false }
    }

    @SubscribeEvent
    fun onRenderPlayer(event: RenderPlayerEvent.Pre) {
        if (! HidePlayers) return
        if (event.entity == mc.thePlayer) return

        event.isCanceled = true
    }
}

