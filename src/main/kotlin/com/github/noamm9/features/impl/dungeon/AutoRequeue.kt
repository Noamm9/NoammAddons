package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.general.PartyHelper
import com.github.noamm9.utils.*
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.location.LocationUtils

object AutoRequeue: Feature() {
    private val requeueCmd by DropdownSetting("Requeue Command", 0, listOf("/joininstance", "/instancerequeue"))
    private val checkParty by ToggleSetting("Check Party", true).withDescription("Should the auto check the party state before running the command.")
    private val minPartySize by SliderSetting("Minimum Party Size", 5, 1, 5, 1).withDescription("Minimum Party Size that the $name will run")
    private val delay by SliderSetting("Delay", 5L, 1L, 10L, 1L).withDescription("Delay in Seconds.")
    private val feedback by ToggleSetting("Feedback", true).withDescription("Print feedback messages from auto in chat.")

    private const val prefix = "&bAutoRequeue &f>"
    private val masterMode get() = if (LocationUtils.isMasterMode) "MASTER_" else ""
    private val floor get() = DungeonUtils.FLOOR_NAMES[LocationUtils.dungeonFloorNumber ?: 0]

    private fun feedBackMessage(msg: String) {
        if (! feedback.value) return
        ThreadUtils.scheduledTask(20) { ChatUtils.modMessage("$prefix $msg") }
    }

    override fun init() {
        register<DungeonEvent.RunEndedEvent>(priority = EventPriority.HIGHEST) {
            if (checkParty.value) {
                if (! PartyUtils.isInParty) return@register feedBackMessage("Not in a party!")
                if (! PartyUtils.isLeader()) return@register feedBackMessage("You are not leader.")
                if (PartyUtils.partySize < minPartySize.value) return@register feedBackMessage("Not enough players in party! (${PartyUtils.partySize}/${minPartySize.value})")
            }
            if (PartyHelper.downtimeList.isNotEmpty()) return@register feedBackMessage("There are players in downtime!")

            ThreadUtils.scheduledTask(delay.value * 20) {
                if (checkParty.value && ! PartyUtils.isLeader()) return@scheduledTask feedBackMessage("You are not the party leader!")
                if (requeueCmd.value == 0) ChatUtils.sendMessage("/joininstance ${masterMode}CATACOMBS${if ((LocationUtils.dungeonFloorNumber ?: 0) > 0) "_FLOOR" else ""}_$floor")
                else ChatUtils.sendMessage("/instancerequeue")
            }
        }
    }
}