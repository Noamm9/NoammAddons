package noammaddons.features.dungeons

import net.minecraft.event.ClickEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.LocationUtils.inDungeons


object ShowExtraStats : Feature() {
    @SubscribeEvent
    fun onChatPacket(event: Chat) {
        if (! inDungeons || ! config.showExtraStats) return
        if (event.component.siblings.any {
                it.chatStyle?.chatClickEvent?.run { action == ClickEvent.Action.RUN_COMMAND && value == "/showextrastats" } == true
            }) {
            event.isCanceled = true
            sendChatMessage("/showextrastats")
        }
    }
}
