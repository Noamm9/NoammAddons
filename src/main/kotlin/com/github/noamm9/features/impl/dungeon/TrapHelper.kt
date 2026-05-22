package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import java.awt.Color
import kotlin.math.abs

object TrapHelper: Feature("Highlights the correct pearl block in New Trap") {

    private val color by ColorSetting("Epearl highlight color", Color.MAGENTA)

    private val northEastRelPos = BlockPos(27, 89, 15)
    private val southWestRelPos = BlockPos(26, 89, 15)
    private var highlightPos: BlockPos? = null

    override fun init() {
        register<DungeonEvent.RoomEvent.onEnter> {
            if (event.room.data.name != "New Trap") {
                highlightPos = null
                return@register
            }
            val prev = ScanUtils.lastKnownRoom ?: return@register
            val direction = getDirectionFromRooms(prev, event.room)
            val corner = event.room.corner ?: return@register
            val rotation = 360 - (event.room.rotation ?: 0)

            val relPos = when (direction) {
                "North", "East" -> northEastRelPos
                "South", "West" -> southWestRelPos
                else -> return@register
            }
            highlightPos = ScanUtils.getRealCoord(relPos, corner, rotation)
        }

        register<RenderWorldEvent> {
            val pos = highlightPos ?: return@register
            Render3D.renderBlock(event.ctx, pos, color.value, outline = true, fill = false, phase = true)
        }

        register<WorldChangeEvent> {
            highlightPos = null
        }
    }

    private fun getDirectionFromRooms(from: UniqueRoom, to: UniqueRoom): String {
        val dx = to.mainRoom.x - from.mainRoom.x
        val dz = to.mainRoom.z - from.mainRoom.z
        return if (abs(dx) >= abs(dz)) {
            if (dx > 0) "East" else "West"
        }
        else {
            if (dz > 0) "South" else "North"
        }
    }
}