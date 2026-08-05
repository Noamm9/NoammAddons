package com.github.noamm9.features.impl.floor7

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D.renderBoxBounds
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object GateHighlight: Feature("Highlights Gate as long as its not destroyed") {
    private val triggerPos = BlockPos(103, 134, 123)
    private val box = aabb(95, 114, 122, 106, 134, 124)

    private val outlineColor by ColorSetting("Outline", Color.CYAN.withAlpha(255))
    private val fillColor by ColorSetting("Fill", Color.CYAN.withAlpha(60))

    private val debugBypass get() = NoammAddons.debugFlags.contains("gate")

    override fun init() {
        register<RenderWorldEvent> {
            if (! debugBypass && (! LocationUtils.inBoss || LocationUtils.F7Phase != 3)) return@register

            val block = level.getBlockState(triggerPos).block
            if (block != Blocks.CRACKED_STONE_BRICKS && block != Blocks.INFESTED_STONE_BRICKS) return@register

            event.ctx.renderBoxBounds(box, outlineColor.value, fillColor.value)
        }
    }
}
