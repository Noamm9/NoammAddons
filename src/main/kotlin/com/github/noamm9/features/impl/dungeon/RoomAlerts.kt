package com.github.noamm9.features.impl.dungeon

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

object RoomAlerts: Feature("Alerts when certain stuff happens in your current room") {
    private val clear by ToggleSetting("Cleared", true)
    private val secrets by ToggleSetting("Secrets Done", true)

    private var clearText = "Cleared"
    private var clearUntil = 0L
    private var secretsUntil = 0L

    override fun init() {
        val clearHud = hudElement("Room Alerts Cleared", { clear.value }, { System.currentTimeMillis() < clearUntil }, centered = true) { ctx, example ->
            val text = if (example) "Cleared" else clearText
            ctx.drawCenteredString(text, 0, 0)
            text.width() to 9f
        }
        val secretsHud = hudElement("Room Alerts Secrets Done", { secrets.value }, { System.currentTimeMillis() < secretsUntil }, centered = true) { ctx, _ ->
            val text = "&aSecrets Done!"
            ctx.drawCenteredString(text, 0, 0)
            text.width() to 9f
        }
        listOf(clearHud, secretsHud).forEachIndexed { index, hud ->
            hud.defaults {
                x = Resolution.width / 2f
                y = Resolution.height * 0.444f + index * 30f
                scale = 2.5f
            }
            hud.defaults.invoke(hud)
        }

        register<WorldChangeEvent> {
            clearUntil = 0L
            secretsUntil = 0L
        }

        register<DungeonEvent.RoomEvent.onStateChange> {
            if (! clear.value && ! secrets.value) return@register
            if (! event.room.data.type.equalsOneOf(RoomType.NORMAL, RoomType.PUZZLE, RoomType.RARE, RoomType.TRAP)) return@register
            if (event.room.data.type == RoomType.PUZZLE && event.room.name != "Blaze") return@register
            if (DungeonListener.thePlayer !in event.roomPlayers) return@register

            when (event.newState) {
                RoomState.CLEARED -> {
                    if (! clear.value) return@register
                    clearText = (if (event.room.data.secrets == 0) "&a" else "") + "Cleared"
                    clearUntil = System.currentTimeMillis() + 2000
                }

                RoomState.GREEN -> {
                    if (! secrets.value || event.room.data.secrets <= 0) return@register
                    secretsUntil = System.currentTimeMillis() + 2000
                }

                else -> return@register
            }
            USound.playSoundStatic(SoundEvents.NOTE_BLOCK_PLING, 0.25f, 1f)
        }
    }
}