package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
//#if CHEAT
import com.github.noamm9.config.types.ToggleSetting
//#endif
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.LeverBlock

object LeverOverlay: Feature(description = "Highlights lever hitboxes") {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    //#if CHEAT
    private val phase by ToggleSetting("See Through Walls")
    //#endif

    private val nearbyLevers = mutableSetOf<BlockPos>()
    private var tickCounter = 0
    private const val RADIUS = 32

    override fun init() {
        register<TickEvent.End> {
            if (! LocationUtils.inDungeon) return@register
            if (tickCounter++ % 10 != 0) return@register

            val center = player.blockPosition()
            val found = mutableSetOf<BlockPos>()

            for (pos in BlockPos.betweenClosed(center.offset(- RADIUS, - RADIUS, - RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
                if (WorldUtils.getBlockAt(pos) is LeverBlock) found.add(pos.immutable())
            }

            nearbyLevers.clear()
            nearbyLevers.addAll(found)
        }

        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon) return@register
            if (nearbyLevers.isEmpty()) return@register

            val outline = mode.value.equalsOneOf(0, 2)
            val fill = mode.value.equalsOneOf(1, 2)

            for (pos in nearbyLevers) event.ctx.renderBlock(
                pos,
                outlineColor.value,
                fillColor.value,
                outline,
                fill,
                //#if CHEAT
                phase.value,
                //#else
                //$false,
                //#endif
                lineWidth.value
            )
        }
    }
}
