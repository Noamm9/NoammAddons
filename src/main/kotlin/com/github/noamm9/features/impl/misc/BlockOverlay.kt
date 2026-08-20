package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.ChoiceConfig
import com.github.noamm9.config.types.ColorConfig
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.EtherwarpHelper
import com.github.noamm9.utils.render.Render3D.renderBlock
import com.github.noamm9.utils.render.RenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents

object BlockOverlay: Feature() {
    private val mode by ChoiceConfig("Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    private val fillColor by ColorConfig("Fill Color", Utils.favoriteColor.withAlpha(50)).hideIf { mode.value == 0 }
    private val outlineColor by ColorConfig("Outline Color", Utils.favoriteColor, false).hideIf { mode.value == 1 }
    private val lineWidth by NumberConfig("Line Width", 2.5, 1, 10, 0.1).hideIf { mode.value == 1 }
    private val phase by BooleanConfig("Phase")
    private val hideDuringEtherwarp by BooleanConfig("Hide with Etherwarp")

    override fun init() {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register { context, blockOutlineContext ->
            if (! enabled) return@register true
            if (mc.options.hideGui) return@register true
            if (hideDuringEtherwarp.value && shouldHide()) return@register false

            RenderContext.fromContext(context).renderBlock(
                blockOutlineContext.pos,
                outlineColor.value,
                fillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase = phase.value,
                lineWidth.value
            )

            false
        }
    }

    private fun shouldHide() = player.isCrouching && EtherwarpHelper.getEtherwarpDistance(player.mainHandItem) != null

}
