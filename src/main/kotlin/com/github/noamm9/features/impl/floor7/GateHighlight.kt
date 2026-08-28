package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

object GateHighlight: Feature("Highlights Gate as long as its not destroyed") {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase")

    private val gates = mapOf(
        1 to Gate(BlockPos(103, 134, 123), aabb(95, 114, 122, 106, 134, 124)),
        2 to Gate(BlockPos(17, 134, 135), aabb(18, 114, 127, 19, 134, 138)),
        3 to Gate(BlockPos(5, 134, 49), aabb(14, 114, 51, 1, 134, 49))
    )

    override fun init() {
        register<RenderWorldEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            val gate = gates[LocationUtils.P3Section] ?: return@register
            if (! WorldUtils.getBlockAt(gate.pos).equalsOneOf(Blocks.CRACKED_STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS)) return@register

            event.ctx.renderBoxBounds(
                gate.box,
                outlineColor.value,
                fillColor.value,
                mode.value.equalsOneOf(1, 2),
                mode.value.equalsOneOf(0, 2),
                phase.value,
                lineWidth.value
            )
        }
    }

    private class Gate(val pos: BlockPos, val box: AABB)
}