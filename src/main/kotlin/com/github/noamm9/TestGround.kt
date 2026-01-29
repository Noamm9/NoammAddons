package com.github.noamm9

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.EntityCheckRenderEvent


object TestGround {
    init {
        EventBus.register<EntityCheckRenderEvent> {
        }
    }
}

