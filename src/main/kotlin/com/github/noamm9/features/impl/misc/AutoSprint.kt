package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature

object AutoSprint: Feature("Automatically sprint for you.") {
    private val disableInWater by ToggleSetting("Disable In Water").withDescription("Stops sprinting while you are in water.")

    override fun init() {
        register<TickEvent.Start> {
            if (mc.screen != null) return@register
            if (player.isSprinting) return@register
            if (disableInWater.value && player.isInWater) return@register
            mc.options.keySprint.isDown = true
        }
    }
}