package com.github.noamm9.event

import com.github.noamm9.event.priority.EventPriority

class EventListener<T: Event>(
    val eventClass: Class<out Event>,
    val priority: EventPriority,
    val receiveCancelled: Boolean = false,
    val callback: EventContext<T>.() -> Unit
) {
    @Volatile var isActive = false

    fun register(): EventListener<T> {
        if (isActive) return this
        isActive = true
        EventBus._registerListener(this)
        return this
    }

    fun unregister(): EventListener<T> {
        if (! isActive) return this
        isActive = false
        EventBus._unregisterListener(this)
        return this
    }
}