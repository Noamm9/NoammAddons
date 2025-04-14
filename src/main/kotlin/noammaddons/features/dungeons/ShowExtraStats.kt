package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.sendChatMessage


object ShowExtraStats: Feature() {
    @SubscribeEvent
    fun onRunEndEvent(event: DungeonEvent.RunEndedEvent) {
        if (! config.showExtraStats) return
        sendChatMessage("/showextrastats")
    }
}
