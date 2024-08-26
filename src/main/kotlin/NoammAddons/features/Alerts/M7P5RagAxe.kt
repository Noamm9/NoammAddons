package NoammAddons.features.Alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.utils.LocationUtils.F7Phase
import NoammAddons.utils.ChatUtils.showTitle
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object M7P5RagAxe {
    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (config.M7P5RagAxe && F7Phase == 5 && event.message.unformattedText.equals("[BOSS] Wither King: You... again?")) {
            showTitle("&1[&6&kO&r&1] &6USE RAGNAROCK AXE! &1[&6&kO&r&1]")
        }
    }
}