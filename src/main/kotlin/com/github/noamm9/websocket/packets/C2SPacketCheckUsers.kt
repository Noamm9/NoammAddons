package com.github.noamm9.websocket.packets

import com.github.noamm9.websocket.PacketRegistry

class C2SPacketCheckUsers: PacketRegistry.WebSocketPacket("check_users") {
    override fun handle() {}
}