package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object AnnounceSpiritLeaps {
    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (config.AnnounceSpiritLeaps.removeFormatting().isNotEmpty()) return
        val message = event.message.unformattedText
        if (message.startsWith("You have teleported to ")) {
            val name = message.replace("You have teleported to ", "").replace("!", "")
            val msg = config.AnnounceSpiritLeaps.removeFormatting().replace("{name}", name)
            mc.thePlayer.sendChatMessage("/pc $msg")
        }
    }
}
