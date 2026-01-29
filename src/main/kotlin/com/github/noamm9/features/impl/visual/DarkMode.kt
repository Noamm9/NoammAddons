package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate

/**
 * @see com.github.noamm9.mixin.MixinGui.onRenderHudPost
 */
object DarkMode: Feature("Darkens the screen") {
    private val opacity by SliderSetting("Opacity", 25, 1, 100, 1)

    @JvmStatic
    fun getOpacity(): Float {
        return opacity.value / 100f
    }
}