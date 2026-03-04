package com.github.noamm9.websocket.packets

import com.github.noamm9.NoammAddons
import com.github.noamm9.websocket.PacketRegistry
import com.github.noamm9.websocket.WebSocket

class C2SPacketPing: PacketRegistry.WebSocketPacket("ping") {
    override fun handle() {
        val myName = NoammAddons.mc.user.name ?: return
        WebSocket.send(S2CPacketPong(myName))
    }
}