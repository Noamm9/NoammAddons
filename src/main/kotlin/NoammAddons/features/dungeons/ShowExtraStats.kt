package NoammAddons.features.dungeons

import net.minecraft.event.ClickEvent
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.LocationUtils.inDungeons

object ShowExtraStats {
    @SubscribeEvent
    fun onChatPacket(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2 || !inDungeons || !config.showExtraStats) return
        if (event.message.siblings.any {
                it.chatStyle?.chatClickEvent?.run { action == ClickEvent.Action.RUN_COMMAND && value == "/showextrastats" } == true
            }) {
            event.isCanceled = true
            mc.thePlayer.sendChatMessage("/showextrastats")
        }
    }
}
