package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.websocket.WebSocketPacket

class S2CPacketSocketInfo(
    var connectedUsers: Int = 0,
    var usersInLobby: Int = 0,
    var lobby: String = ""
): WebSocketPacket("socket_info") {
    override fun handle() {
        ChatUtils.chat("§b§m--------------------------------")
        ChatUtils.chat("§6§lWebSocket Stats")
        ChatUtils.chat("")
        ChatUtils.chat(" §fTotal Online: §a$connectedUsers")
        ChatUtils.chat(" §fUsers in your World: §a$usersInLobby")
        ChatUtils.chat(" §fCurrent Hash: §7$lobby")
        ChatUtils.chat("§b§m--------------------------------")
    }
}