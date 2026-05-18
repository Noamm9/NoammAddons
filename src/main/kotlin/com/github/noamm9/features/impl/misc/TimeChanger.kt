package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import java.time.LocalTime

/**
 * @see com.github.noamm9.mixin.MixinClientPacketListener
 */
object TimeChanger: Feature("Changes the world time.") {
    private val timeChangerMode by DropdownSetting("Time", 0, listOf("Day", "Noon", "Sunset", "Night", "Midnight", "Sunrise", "Real Time"))
    private val TIME_VALUES = longArrayOf(1000L, 6000L, 12000L, 13000L, 18000L, 23000L)

    @JvmStatic
    fun setTime() {
        val customTime = TIME_VALUES.getOrElse(timeChangerMode.value) { getTickTime() }
        mc.level?.setTimeFromServer(mc.level !!.gameTime, customTime, false)
    }

    private fun getTickTime(): Long {
        val now = LocalTime.now()
        var ticks = (now.hour * 1000) + (now.minute * 16.66).toLong() - 6000
        if (ticks < 0) ticks += 24000
        return ticks
    }
}