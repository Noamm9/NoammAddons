package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.websocket.WebSocketPacket

class S2CPacketRoomSecrets(val room: String, val secrets: Int): WebSocketPacket {
    override fun handle() {
        DungeonScanner.uniqueRooms[room]?.let {
            if (it.foundSecrets < secrets) it.foundSecrets = secrets
        }
    }
}