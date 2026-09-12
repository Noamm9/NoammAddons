package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import net.minecraft.core.BlockPos
import java.awt.Color

object HealerBreak: Feature(name = "Healer 3x3", description = "Highlights the blocks healers are ment to break during the goldor fight phase") {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val outlineColor by ColorSetting("Outline Color", Color.red, false).hideIf { mode.value == 1 }
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase")

    private val blockPos = listOf(
        BlockPos(54, 63, 113), BlockPos(55, 63, 113), BlockPos(53, 63, 113),
        BlockPos(54, 63, 114), BlockPos(55, 63, 114), BlockPos(53, 63, 114),
        BlockPos(54, 63, 115), BlockPos(55, 63, 115), BlockPos(53, 63, 115),
    )

    override fun init() {
        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon) return@register
            if (LocationUtils.F7Phase != 4) return@register

            blockPos.forEach { pos ->
                event.ctx.renderBlock(
                    pos,
                    outlineColor.value,
                    mode.value.equalsOneOf(0, 2),
                    mode.value.equalsOneOf(1, 2),
                    phase.value,
                    lineWidth.value
                )
            }
        }
    }
}