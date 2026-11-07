package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.EtherwarpHelper
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import com.github.noamm9.utils.render.world.RenderContext
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState

object BlockOverlay: Feature() {
    private val mode by DropdownSetting("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }
    private val lineWidth by SliderSetting("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by ToggleSetting("Phase")
    private val hideDuringEtherwarp by ToggleSetting("Hide with Etherwarp")

    @JvmStatic
    fun renderOverlay(ctx: RenderContext, state: BlockOutlineRenderState) {
        if (mc.gui.hud.isHidden) return
        if (hideDuringEtherwarp.value && shouldHide()) return

        ctx.renderBlock(
            state.pos,
            outlineColor.value,
            fillColor.value,
            mode.value.equalsOneOf(0, 2),
            mode.value.equalsOneOf(1, 2),
            phase = phase.value,
            lineWidth.value
        )
    }

    private fun shouldHide() = player.isCrouching && EtherwarpHelper.getEtherwarpDistance(player.mainHandItem) != null

}
