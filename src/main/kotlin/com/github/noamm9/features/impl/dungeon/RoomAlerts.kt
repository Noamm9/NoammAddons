package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.equalsOneOf
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents

object RoomAlerts: Feature("Alerts when certain stuff happens in your current room") {
    private val clear by ToggleSetting("Cleared", true)
    private val secrets by ToggleSetting("Secrets Done", true)

    override fun init() {
        register<DungeonEvent.RoomEvent.onStateChange> {
            if (! event.room.data.type.equalsOneOf(RoomType.NORMAL, RoomType.PUZZLE, RoomType.RARE, RoomType.TRAP)) return@register
            if (event.room.data.type == RoomType.PUZZLE && event.room.name != "Blaze") return@register
            if (DungeonListener.thePlayer !in event.roomPlayers) return@register

            when (event.newState) {
                RoomState.CLEARED -> if (clear.value) {
                    alert((if (event.room.data.secrets == 0) "&a" else "") + "Cleared")
                }

                RoomState.GREEN -> if (secrets.value && event.room.data.secrets > 0) {
                    alert("&aSecrets Done!")
                }

                else -> return@register
            }
        }
    }

    private fun alert(msg: String) {
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1f))
        ChatUtils.showTitle(msg)
    }
}