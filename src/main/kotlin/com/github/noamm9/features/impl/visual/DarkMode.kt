package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.withDescription

/**
 * @see com.github.noamm9.mixin.MixinGui.onRenderHudPost
 */
object DarkMode: Feature("Darkens the screen") {
    val renderOverGUI by ToggleSetting("Render over GUI").withDescription("Toggles the darkening of Hotbar, Tablist etc.")
    private val opacity by SliderSetting("Opacity", 25, 1, 100, 1)

    @JvmStatic
    fun getOpacity(): Float {
        return opacity.value / 100f
    }
}