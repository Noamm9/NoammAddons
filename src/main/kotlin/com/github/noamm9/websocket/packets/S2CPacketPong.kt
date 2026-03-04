package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.websocket.PacketRegistry

class S2CPacketPong(var username: String = ""): PacketRegistry.WebSocketPacket("pong") {
    override fun handle() = ChatUtils.modMessage("&bFound User: &d$username")
}