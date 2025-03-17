package noammaddons.features.dungeons

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.features.general.PartyCommands
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.noFormatText

object AutoRequeue: Feature() {
    private const val prefix = "&bAutoRequeue &f>"
    private val masterMode get() = if (LocationUtils.isMasterMode) "MASTER_" else ""
    private val floor get() = PartyCommands.NUMBERS_TO_TEXT[LocationUtils.dungeonFloorNumber ?: 0]
    private var shouldIgnore = false

    private fun feedBackMessage(msg: String) {
        if (! config.autoRequeueFeedback) return
        ChatUtils.modMessage("$prefix $msg")
    }

    init {
        DungeonUtils.dungeonEnded.onSetValue { value ->
            if (! config.autoRequeue) return@onSetValue
            if (! value) return@onSetValue
            if (PartyCommands.downtimeList.isNotEmpty()) return@onSetValue feedBackMessage("There are players in downtime!")

            scope.launch {
                delay(config.autoRequeueDelay.toLong())
                if (! PartyUtils.isPartyLeader()) return@launch feedBackMessage("You are not the party leader!")
                if (shouldIgnore) {
                    shouldIgnore = false
                    return@launch feedBackMessage("Ignoring player leaving")
                }

                ChatUtils.sendChatMessage("/joininstance ${masterMode}CATACOMBS_FLOOR_${floor}")
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.autoRequeue) return
        if (! config.disableAutoRequeueOnLeave) return
        val ignorePatterns = PartyUtils.memberLeftPatterns + PartyUtils.disbandedPattern
        if (ignorePatterns.none { it.matches(event.component.noFormatText) }) return
        shouldIgnore = true
    }
}
