package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage


object AnnounceSpiritLeaps: Feature() {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (config.AnnounceSpiritLeaps.removeFormatting().isEmpty()) return
        val message = event.component.noFormatText
        if (message.startsWith("You have teleported to ")) {
            val name = message.replace("You have teleported to ", "").replace("!", "")
            val msg = config.AnnounceSpiritLeaps.removeFormatting().replace("{name}", name)
            sendPartyMessage(msg)
        }
    }
}
