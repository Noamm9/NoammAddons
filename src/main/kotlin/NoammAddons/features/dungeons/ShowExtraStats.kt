package NoammAddons.features.dungeons

import net.minecraft.event.ClickEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.Chat
import NoammAddons.utils.LocationUtils.inDungeons


object ShowExtraStats {
    @SubscribeEvent
    fun onChatPacket(event: Chat) {
        if (!inDungeons || !config.showExtraStats) return
        if (event.component.siblings.any {
                it.chatStyle?.chatClickEvent?.run { action == ClickEvent.Action.RUN_COMMAND && value == "/showextrastats" } == true
            }) {
            event.isCanceled = true
            mc.thePlayer.sendChatMessage("/showextrastats")
        }
    }
}
