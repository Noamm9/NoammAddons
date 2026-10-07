package com.github.noamm9.utils.dungeons

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.location.LocationUtils.dungeonFloorNumber
import com.github.noamm9.utils.location.LocationUtils.inBoss

object F7Listener: ISelfInit {
    private var necronDead = false
    private var necronStart = false
    private var witherKingStart = false

    override fun init() {
        EventBus.register<ChatMessageEvent> {
            val msg = event.unformattedText

            if (msg == "[BOSS] Necron: ARGH!") {
                necronStart = true
                return@register
            }
        }

        EventBus.register<BossBarUpdateEvent> {
            if (dungeonFloorNumber != 7 || ! inBoss) return@register
            if (event.progress > 0f) return@register
            val name = event.name.unformattedText

            if (name.contains("Necron") && ! necronDead && necronStart) {
                EventBus.post(DungeonEvent.NecronDeathEvent)
                necronDead = true
            }

            if (name.contains("wither king", ignoreCase = true) && ! witherKingStart) {
                EventBus.post(DungeonEvent.WitherKingStartEvent)
                witherKingStart = true
            }
        }

        EventBus.register<WorldChangeEvent> {
            necronDead = false
            necronStart = false
            witherKingStart = false
        }
    }
}