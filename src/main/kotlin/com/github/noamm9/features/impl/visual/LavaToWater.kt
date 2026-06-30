package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import java.awt.Color

object LavaToWater : Feature("Replaces lava with the water texture and water fog (resource-pack aware).") {
    val colorTint by ToggleSetting("Color Tint", false).onChange { refreshIfActive() }
    val tintColor by ColorSetting("Tint Color", Color(0x3F, 0x76, 0xE4), false)
        .showIf { colorTint.value }
        .onChange { refreshIfActive() }

    private fun refreshIfActive() {
        if (enabled && mc.level != null) mc.levelRenderer.allChanged()
    }

    override fun onEnable() {
        super.onEnable()
        if (mc.level != null) mc.levelRenderer.allChanged()
    }

    override fun onDisable() {
        super.onDisable()
        if (mc.level != null) mc.levelRenderer.allChanged()
    }
}