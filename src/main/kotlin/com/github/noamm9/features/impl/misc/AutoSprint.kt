package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature

object AutoSprint: Feature("Automatically sprint for you.") {
    override fun init() {
        register<TickEvent.Start> {
            if (mc.player?.isSprinting != false) return@register
            mc.options.keySprint.isDown = true
        }
    }
}