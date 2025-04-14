package noammaddons.features.dungeons

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.features.general.PartyCommands
import noammaddons.utils.*

object AutoRequeue: Feature() {
    private const val prefix = "&bAutoRequeue &f>"
    private val masterMode get() = if (LocationUtils.isMasterMode) "MASTER_" else ""
    private val floor get() = PartyCommands.NUMBERS_TO_TEXT[LocationUtils.dungeonFloorNumber ?: 0]
    private var shouldIgnore = false

    private fun feedBackMessage(msg: String) {
        if (! config.autoRequeueFeedback) return
        ChatUtils.modMessage("$prefix $msg")
    }

    @SubscribeEvent
    fun onRunEnd(event: DungeonEvent.RunEndedEvent) {
        if (! config.autoRequeue) return
        if (! PartyUtils.inParty) return feedBackMessage("Not in a party!")
        if (PartyUtils.size != 5) return feedBackMessage("Not enough players in party!")
        if (PartyCommands.downtimeList.isNotEmpty()) return feedBackMessage("There are players in downtime!")

        scope.launch {
            delay(config.autoRequeueDelay.toLong())
            if (PartyUtils.leader != mc.session.username) return@launch feedBackMessage("You are not the party leader!")
            if (shouldIgnore) {
                shouldIgnore = false
                return@launch feedBackMessage("Ignoring player leaving")
            }

            ChatUtils.sendChatMessage("/joininstance ${masterMode}CATACOMBS_FLOOR_${floor}")
        }
    }
}
