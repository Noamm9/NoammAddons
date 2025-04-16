package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.Utils.remove

// imagine using regex
object AnnounceSpiritLeaps: Feature() {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (config.AnnounceSpiritLeaps.removeFormatting().isEmpty()) return
        val message = event.component.noFormatText
        if (! message.startsWith("You have teleported to ")) return
        val name = message.remove("You have teleported to ", "!")
        val msg = config.AnnounceSpiritLeaps.removeFormatting().replace("{name}", name)
        sendPartyMessage(msg)
    }
}
