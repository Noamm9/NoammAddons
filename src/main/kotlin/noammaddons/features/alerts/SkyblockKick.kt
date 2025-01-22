package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage

object SkyblockKick: Feature() {
    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.SBKick) return
        if (event.component.noFormatText != "You were kicked while joining that server!") return

        sendPartyMessage("${CHAT_PREFIX.removeFormatting()} You were kicked while joining that server!")
    }
}