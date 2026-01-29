package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import java.time.LocalTime

object TimeChanger: Feature("Changes the world time") {
    private val timeChangerMode by DropdownSetting("Time", 0, listOf("Day", "Noon", "Sunset", "Night", "Midnight", "Sunrise", "Real Time"))
    private val TIME_VALUES = longArrayOf(1000L, 6000L, 12000L, 13000L, 18000L, 23000L)

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre>(EventPriority.LOW) {
            if (event.packet !is ClientboundSetTimePacket) return@register
            val customTime = TIME_VALUES.getOrElse(timeChangerMode.value) { getTickTime() }
            mc.level?.setTimeFromServer(mc.level !!.gameTime, customTime, false)
            event.isCanceled = true
        }
    }

    private fun getTickTime(): Long {
        val now = LocalTime.now()
        var ticks = (now.hour * 1000) + (now.minute * 16.66).toLong() - 6000
        if (ticks < 0) ticks += 24000
        return ticks
    }
}