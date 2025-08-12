package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.features.impl.general.PartyCommands
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.*
import noammaddons.utils.ThreadUtils.setTimeout

object AutoRequeue: Feature() {
    private val checkParty by ToggleSetting("Check Party", true)
    private val delay by SliderSetting("Delay", 0L, 10L, 1L, 5L)
    private val feedback by ToggleSetting("Feedback", true)

    private const val prefix = "&bAutoRequeue &f>"
    private val masterMode get() = if (LocationUtils.isMasterMode) "MASTER_" else ""
    private val floor get() = PartyCommands.NUMBERS_TO_TEXT[LocationUtils.dungeonFloorNumber ?: 0]

    private fun feedBackMessage(msg: String) {
        if (! feedback) return
        setTimeout(1000) { ChatUtils.modMessage("$prefix $msg") }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRunEnd(event: DungeonEvent.RunEndedEvent) {
        if (checkParty) {
            if (! PartyUtils.inParty) return feedBackMessage("Not in a party!")
            if (PartyUtils.size != 5) return feedBackMessage("Not enough players in party!")
            if (PartyCommands.downtimeList.isNotEmpty()) return feedBackMessage("There are players in downtime!")
        }

        setTimeout(delay * 1000) {
            if (checkParty && PartyUtils.leader != mc.session.username) return@setTimeout feedBackMessage("You are not the party leader!")
            ChatUtils.sendChatMessage("/joininstance ${masterMode}CATACOMBS_FLOOR_${floor}")
        }
    }
}
