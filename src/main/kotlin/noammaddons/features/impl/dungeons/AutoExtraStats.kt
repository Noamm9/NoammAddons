package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.sendChatMessage


object AutoExtraStats: Feature("Automatically sends /showextrastats at the end of the run") {
    @SubscribeEvent
    fun onRunEndEvent(event: DungeonEvent.RunEndedEvent) = sendChatMessage("/showextrastats")
}
