package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object AnnounceSpiritLeaps {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (config.AnnounceSpiritLeaps.removeFormatting().isEmpty()) return
        val message = event.component.unformattedText.removeFormatting()
        if (message.startsWith("You have teleported to ")) {
            val name = message.replace("You have teleported to ", "").replace("!", "")
            val msg = config.AnnounceSpiritLeaps.removeFormatting().replace("{name}", name)
            sendChatMessage("/pc $msg")
        }
    }
}
