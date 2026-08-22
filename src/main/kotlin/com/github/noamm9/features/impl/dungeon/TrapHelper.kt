package com.github.noamm9.features.impl.dungeon

/*

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.ColorConfig
import com.github.noamm9.config.types.ChoiceConfig
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D
import net.minecraft.core.BlockPos

object TrapHelper: Feature("Highlights the correct pearl block in New Trap") {
    private val mode by ChoiceConfig("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val fillColor by ColorConfig("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }
    private val outlineColor by ColorConfig("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase")

    private var highlightPos: BlockPos? = null

    override fun init() {
        register<WorldChangeEvent> { highlightPos = null }

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
            if (LocationUtils.inBoss) return@register
            val pos = highlightPos ?: return@register

            Render3D.renderBlock(
                event.ctx, pos,
                outlineColor.value,
                fillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase = phase.value,
            )
        }
    }
}

 */