package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.general.PartyHelper
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.location.LocationUtils

object AutoRequeue: Feature() {
    private val checkParty by ToggleSetting("Check Party", true).withDescription("Should the auto check the party state before running the command.")
    private val delay by SliderSetting("Delay", 5L, 1L, 10L, 1L).withDescription("Delay in Seconds.")
    private val feedback by ToggleSetting("Feedback", true).withDescription("Print feedback messages from auto in chat.")

    private const val prefix = "&bAutoRequeue &f>"
    private val masterMode get() = if (LocationUtils.isMasterMode) "MASTER_" else ""
    private val floor get() = DungeonUtils.FLOOR_NAMES[LocationUtils.dungeonFloorNumber ?: 0]

    private fun feedBackMessage(msg: String) {
        if (! feedback.value) return
        ThreadUtils.setTimeout(1000) { ChatUtils.modMessage("$prefix $msg") }
    }

    override fun init() {
        register<DungeonEvent.RunEndedEvent> {
            if (checkParty.value) {
                if (! PartyUtils.isInParty) return@register feedBackMessage("Not in a party!")
                if (PartyUtils.members.size != 5) return@register feedBackMessage("Not enough players in party!")
                if (PartyHelper.downtimeList.isNotEmpty()) return@register feedBackMessage("There are players in downtime!")
            }

            ThreadUtils.setTimeout(delay.value * 1000) {
                if (checkParty.value && ! PartyUtils.isLeader()) return@setTimeout feedBackMessage("You are not the party leader!")
                ChatUtils.sendMessage("/joininstance ${masterMode}CATACOMBS_FLOOR_${floor}")
            }
        }
    }
}