package com.github.noamm9.event.impl

import com.github.noamm9.event.Event

sealed class WebSocketEvent: Event(cancelable = false) {
    class Payload(val message: String): WebSocketEvent()
    object Connect: WebSocketEvent()
    object Disconnect: WebSocketEvent()
}