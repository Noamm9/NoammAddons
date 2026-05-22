package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import java.awt.Color

object TrapHelper: Feature("Highlights the correct pearl block in New Trap") {

    private val color by ColorSetting("Epearl highlight color", Color.MAGENTA)
    private var highlightPos: BlockPos? = null

    override fun init() {
        register<DungeonEvent.RoomEvent.onEnter> {
            if (event.room.data.name != "New Trap") {
                highlightPos = null;
                return@register
            }
            val corner = event.room.corner ?: return@register
            val rotation = event.room.rotation ?: return@register

            val relPos = when (rotation) {
                0, 90 -> BlockPos(26, 89, 15)
                180, 270 -> BlockPos(27, 89, 15)
                else -> return@register
            }
            highlightPos = ScanUtils.getRealCoord(relPos, corner, 360 - rotation)
        }

        register<RenderWorldEvent> {
            val pos = highlightPos ?: return@register
            Render3D.renderBlock(event.ctx, pos, color.value, outline = true, fill = false, phase = true)
        }

        register<WorldChangeEvent> { highlightPos = null }
    }
}