package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.withAlpha
import gg.essential.universal.UResolution
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

/**
 * @see com.github.noamm9.mixin.MixinGui.onRenderHudPre
 * @see com.github.noamm9.mixin.MixinGui.onRenderHudPost
 */
object DarkMode: Feature("Darkens the screen") {
    private val opacity by SliderSetting("Opacity", 25, 1, 80, 1, "%").withDescription("The strength of the dark tint.")

    @JvmStatic
    val tintHud by ToggleSetting("Tint HUD").withDescription("Should the dark tint also apply to HUD elements?")

    @JvmStatic
    fun drawOverlay(ctx: GuiGraphicsExtractor) {
        if (enabled) ctx.fill(
            0, 0,
            UResolution.scaledWidth,
            UResolution.scaledHeight,
            Color.BLACK.withAlpha(opacity.value / 100f).rgb
        )
    }
}