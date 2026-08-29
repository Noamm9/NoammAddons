package com.github.noamm9.event

class EventContext<T: Event>(val event: T, var listener: EventListener<T>)