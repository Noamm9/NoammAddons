package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.config.PogObject
import com.github.noamm9.features.Feature

object SoundManager: Feature("Adjust volumes for every sound in the game") {
    var volumes by PogObject("noammaddons_sounds", mutableMapOf<String, Float>())

    @JvmStatic
    fun getMultiplier(id: String): Float {
        if (! enabled) return 1.0f
        return volumes.getOrDefault(id, 1f)
    }
}