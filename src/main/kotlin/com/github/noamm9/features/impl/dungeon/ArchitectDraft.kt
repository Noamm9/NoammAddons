package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils

object ArchitectDraft: Feature("Architect Draft") {
    private val sayDraft by ToggleSetting("Announce Draft", true)

    //#if CHEAT
    private val autoDraft by ToggleSetting("Auto Draft", true)
    //#endif

    private val resetPattern = Regex("^You used the Architect's First Draft to reset (.+)!$")
    private val failPattern1 = Regex("^PUZZLE FAIL! (?<player>\\w{1,16}) .+$")
    private val failPattern2 = Regex("^\\[STATUE] Oruo the Omniscient: (?<player>\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$")

    override fun init() {
        register<ChatMessageEvent> {
            if (! LocationUtils.inDungeon || LocationUtils.inBoss) return@register
            if (ScanUtils.currentRoom?.data?.type != RoomType.PUZZLE) return@register
            val msg = event.unformattedText

            if (sayDraft.value) resetPattern.find(msg)?.destructured?.component1()?.let { puzzleName ->
                ChatUtils.sendPartyMessage("Used Draft to Reset $puzzleName")
            }

            //#if CHEAT
            if (autoDraft.value) {
                val match = failPattern1.find(msg) ?: failPattern2.find(msg)
                val name = match?.groups?.get("player")?.value
                if (name == mc.user.name) ThreadUtils.setTimeout(1500) {
                    mc.player?.connection?.sendChat("/gfs ARCHITECT_FIRST_DRAFT 1")
                }
            }
            //#endif
        }
    }
}