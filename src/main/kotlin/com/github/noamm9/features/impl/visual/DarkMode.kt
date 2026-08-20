package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.ColorUtils.withAlpha
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

/**
 * @see com.github.noamm9.mixin.MixinGui.onRenderHudPre
 * @see com.github.noamm9.mixin.MixinGui.onRenderHudPost
 */
object DarkMode: Feature("Darkens the screen") {
    private val opacity by NumberConfig("Opacity", 25, 1, 100, 1).withDescription("The strength of the dark tint.")

    @JvmStatic
    val tintHud by BooleanConfig("Tint HUD").withDescription("Should the dark tint also apply to HUD elements?")

    @JvmStatic
    fun drawOverlay(ctx: GuiGraphicsExtractor) {
        if (! enabled) return
        val window = mc.window
        ctx.fill(
            0, 0,
            window.guiScaledWidth,
            window.guiScaledHeight,
            Color.BLACK.withAlpha(opacity.value / 100f).rgb
        )
    }
}