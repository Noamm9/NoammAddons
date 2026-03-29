package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.websocket.WebSocketPacket

class S2CPacketChat(val message: String): WebSocketPacket("chat") {
    override fun handle() = ChatUtils.chat("§b[WS]§r $message")
}