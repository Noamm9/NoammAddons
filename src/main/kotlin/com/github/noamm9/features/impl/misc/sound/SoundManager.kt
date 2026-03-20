package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.PogObject
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.gui.SoundManagerScreen

object SoundManager: Feature("Adjust volumes for every sound in the game") {
    val volumes = PogObject("noammaddons_sounds", mutableMapOf<String, Float>())

    val btn by ButtonSetting("Open SoundManager GUI") {
        NoammAddons.screen = SoundManagerScreen()
    }

    @JvmStatic
    fun getMultiplier(id: String): Float {
        if (! enabled) return 1.0f
        return volumes.getData().getOrDefault(id, 1f)
    }
}