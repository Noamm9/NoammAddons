package noammaddons.features.dungeons

import net.minecraft.event.ClickEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.LocationUtils.inDungeons


object ShowExtraStats {
    @SubscribeEvent
    fun onChatPacket(event: Chat) {
        if (!inDungeons || !config.showExtraStats) return
        if (event.component.siblings.any {
                it.chatStyle?.chatClickEvent?.run { action == ClickEvent.Action.RUN_COMMAND && value == "/showextrastats" } == true
            }) {
            event.isCanceled = true
            sendChatMessage("/showextrastats")
        }
    }
}
