package com.github.noamm9.features.impl.tweaks

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.hideIf
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.renderer.state.BlockOutlineRenderState

object BlockOverlay: Feature() {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase")

    override fun init() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register { context, blockOutlineContext ->
            if (! enabled) return@register true
            render(context, blockOutlineContext)
            false
        }
    }

    fun render(ctx: WorldRenderContext, blockCtx: BlockOutlineRenderState) {
        if (mc.options.hideGui) return

        Render3D.renderBlock(
            RenderContext.fromContext(ctx),
            blockCtx.pos,
            outlineColor.value,
            fillColor.value,
            mode.value.equalsOneOf(0, 2),
            mode.value.equalsOneOf(1, 2),
            phase = phase.value,
            lineWidth.value
        )
    }
}
