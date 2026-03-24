package com.github.noamm9.websocket.packets

import com.github.noamm9.websocket.PacketRegistry

class C2SPacketLobbyPing: PacketRegistry.WebSocketPacket("lobby_ping") {
    override fun handle() {}
}