package com.github.noamm9.websocket.packets

import com.github.noamm9.websocket.PacketRegistry

@Suppress("UNUSED")
class C2SPacketServerHash(val hash: Int): PacketRegistry.WebSocketPacket("server_hash") {
    override fun handle() {}
}