package com.github.noamm9.features.impl.misc.sound

import com.github.noamm9.config.PogObject
import com.github.noamm9.features.Feature

object SoundManager: Feature("Adjust volumes for every sound in the game") {
    val volumes = PogObject("noammaddons_sounds", mutableMapOf<String, Float>())

    @JvmStatic
    fun getMultiplier(id: String): Float {
        if (! enabled) return 1.0f
        return volumes.get().getOrDefault(id, 1f)
    }
}