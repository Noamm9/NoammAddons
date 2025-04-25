package noammaddons.features.impl.dungeons

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.features.impl.general.PartyCommands
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*

object AutoRequeue: Feature() {
    private val checkParty = ToggleSetting("Check Party", true).register1()
    private val delay = SliderSetting("Delay", 0, 10_000, 5000.0).register1()
    private val feedback = ToggleSetting("Feedback", true).register1()


    private const val prefix = "&bAutoRequeue &f>"
    private val masterMode get() = if (LocationUtils.isMasterMode) "MASTER_" else ""
    private val floor get() = PartyCommands.NUMBERS_TO_TEXT[LocationUtils.dungeonFloorNumber ?: 0]

    private fun feedBackMessage(msg: String) {
        if (! feedback.value) return
        ChatUtils.modMessage("$prefix $msg")
    }

    @SubscribeEvent
    fun onRunEnd(event: DungeonEvent.RunEndedEvent) {
        if (checkParty.value) {
            if (! PartyUtils.inParty) return feedBackMessage("Not in a party!")
            if (PartyUtils.size != 5) return feedBackMessage("Not enough players in party!")
            if (PartyCommands.downtimeList.isNotEmpty()) return feedBackMessage("There are players in downtime!")
        }

        scope.launch {
            delay(delay.value.toLong())
            if (checkParty.value && PartyUtils.leader != mc.session.username) return@launch feedBackMessage("You are not the party leader!")
            ChatUtils.sendChatMessage("/joininstance ${masterMode}CATACOMBS_FLOOR_${floor}")
        }
    }
}
