package com.github.noamm9.websocket

interface WebSocketPacket {
    fun handle() = Unit
}