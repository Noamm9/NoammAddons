package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D.renderBoxBounds
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import java.awt.Color

object GateHighlight: Feature("Highlights Gate as long as its not destroyed") {
    private data class Gate(val pos: BlockPos, val box: AABB)

    private val gates = mapOf(
        1 to Gate(BlockPos(103, 134, 123), aabb(95, 114, 122, 106, 134, 124)),
        2 to Gate(BlockPos(17, 134, 135), aabb(18, 114, 126, 19, 134, 138)),
        3 to Gate(BlockPos(5, 134, 49), aabb(14, 114, 51, 1, 134, 49))
    )

    private val outlineColor by ColorSetting("Outline", Color.CYAN.withAlpha(255))
    private val fillColor by ColorSetting("Fill", Color.CYAN.withAlpha(60))

    override fun init() {
        register<RenderWorldEvent> {
            if (! LocationUtils.inBoss || LocationUtils.F7Phase != 3) return@register
            val gate = gates[LocationUtils.P3Section] ?: return@register

            val block = level.getBlockState(gate.pos).block
            if (block != Blocks.CRACKED_STONE_BRICKS && block != Blocks.INFESTED_STONE_BRICKS) return@register

            event.ctx.renderBoxBounds(gate.box, outlineColor.value, fillColor.value)
        }
    }
}
