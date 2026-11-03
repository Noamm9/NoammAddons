package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.features.Feature
import java.time.LocalTime

/**
 * @see com.github.noamm9.mixin.MixinSkyRenderer
 * @see com.github.noamm9.mixin.MixinAtmosphericFogEnvironment
 * @see com.github.noamm9.mixin.MixinAttributeTrackSampler
 */
object TimeChanger: Feature("Changes the world time.") {
    private val timeChangerMode by DropdownSetting("Time", 0, listOf("Day", "Noon", "Sunset", "Night", "Midnight", "Sunrise", "Real Time"))
    private val TIME_VALUES = longArrayOf(1000L, 6000L, 12000L, 13000L, 18000L, 23000L)

    @JvmField var extractingSky = false

    @JvmStatic fun getTime() = TIME_VALUES.getOrElse(timeChangerMode.value) { getTickTime() }
    private fun getTickTime() = LocalTime.now().let { Math.floorMod(it.hour * 1000L + (it.minute * 16.66).toLong() - 6000, 24000L) }
}