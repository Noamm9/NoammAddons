package com.github.noamm9.websocket

abstract class WebSocketPacket(val type: String) {
    abstract fun handle()
}