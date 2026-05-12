package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.TextInputSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.height
import com.github.noamm9.utils.render.Render2D.width
import java.awt.Color
import kotlin.math.abs

object TrapRoomDirection : Feature("tells where to pearl in New Trap") {
    private const val TICKS_PER_SECOND = 20

    private val color by ColorSetting("Title Color", Color.WHITE)
    private val duration by SliderSetting("Duration", 3f, 1f, 10f, 0.5f, "s")
    private val showOnReEnter by ToggleSetting("Show on Re-Enter").withDescription("Show the direction every time you re-enter the trap room, not just the first time.")
    private val southWestText by TextInputSetting("South / West Text", "mid")
    private val northEastText by TextInputSetting("North / East Text", "left")
    private val resetButton by ButtonSetting("Reset to Default") {
        southWestText.value = "mid"
        northEastText.value = "left"
    }

    private var lastTrapRoom: UniqueRoom? = null
    private var displayText = ""
    private var displayTicks = 0

    override fun init() {
        hudElement(
            "Trap Direction",
            shouldDraw = { LocationUtils.inDungeon && displayTicks > 0 }
        ) { ctx, example ->
            val text = if (example) "mid" else displayText
            Render2D.drawString(ctx, text, 0, 0, color.value, 2.5)
            text.width() * 2.5f to text.height() * 2.5f
        }

        register<DungeonEvent.RoomEvent.onEnter> {
            if (event.room.data.type != RoomType.TRAP) return@register
            if (!showOnReEnter.value && event.room === lastTrapRoom) return@register

            lastTrapRoom = event.room

            val prev = ScanUtils.lastKnownRoom ?: return@register
            val direction = getDirectionFromRooms(prev, event.room)

            displayText = when (direction) {
                "South", "West" -> southWestText.value
                "North", "East" -> northEastText.value
                else -> return@register
            }
            displayTicks = (duration.value * TICKS_PER_SECOND).toInt()
        }

        register<TickEvent.Start> {
            if (displayTicks > 0) displayTicks--
        }

        register<WorldChangeEvent> {
            lastTrapRoom = null
            displayTicks = 0
        }
    }

    private fun getDirectionFromRooms(from: UniqueRoom, to: UniqueRoom): String {
        val dx = to.mainRoom.x - from.mainRoom.x
        val dz = to.mainRoom.z - from.mainRoom.z
        return if (abs(dx) >= abs(dz)) {
            if (dx > 0) "East" else "West"
        } else {
            if (dz > 0) "South" else "North"
        }
    }
}
