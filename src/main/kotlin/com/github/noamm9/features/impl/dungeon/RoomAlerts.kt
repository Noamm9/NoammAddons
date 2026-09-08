package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.USound
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToLong

object RoomAlerts: Feature("Alerts when certain stuff happens in your current room") {
    private val clear by ToggleSetting("Cleared", true)
    private val secrets by ToggleSetting("Secrets Done", true)
    private val displayTime by SliderSetting("Display Time", 2.0, 0.5, 3.0, 0.1, "s")

    private var alertText = "Cleared"
        set(value) {
            field = value
            USound.playSoundStatic(SoundEvents.NOTE_BLOCK_PLING, 0.25f, 1f)
            alertUntil = System.currentTimeMillis() + (displayTime.value * 1000).roundToLong()
        }

    private var alertUntil = 0L

    override fun init() {
        hudElement("Room Alerts", { clear.value || secrets.value }, { System.currentTimeMillis() < alertUntil }, centered = true) { ctx, example ->
            val text = if (example) "Cleared" else alertText
            ctx.drawCenteredString(text, 0, 0)
            text.width() to 9f
        } defaults {
            x = Resolution.width / 2f
            y = Resolution.height * 0.444f
            scale = 2.5f
        }

        register<DungeonEvent.RoomEvent.onStateChange> {
            if (! clear.value && ! secrets.value) return@register
            if (! event.room.data.type.equalsOneOf(RoomType.NORMAL, RoomType.PUZZLE, RoomType.RARE, RoomType.TRAP)) return@register
            if (event.room.data.type == RoomType.PUZZLE && event.room.name != "Blaze") return@register
            if (DungeonListener.thePlayer !in event.roomPlayers) return@register

            alertText = when (event.newState) {
                RoomState.CLEARED if clear.value -> (if (event.room.data.secrets == 0) "&a" else "") + "Cleared"
                RoomState.GREEN if (secrets.value && event.room.data.secrets > 0) -> "&aSecrets Done!"
                else -> return@register
            }
        }

        register<WorldChangeEvent> { alertUntil = 0L }
    }
}