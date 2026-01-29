package com.github.noamm9.event.impl

import com.github.noamm9.event.Event

abstract class TickEvent: Event(false) {
    object Start: TickEvent()
    object End: TickEvent()

    object Server: TickEvent()
}