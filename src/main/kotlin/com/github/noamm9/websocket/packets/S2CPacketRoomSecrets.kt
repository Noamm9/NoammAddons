package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.websocket.WebSocketPacket

class S2CPacketRoomSecrets(val room: String, val secrets: Int): WebSocketPacket("dungeonroomsecrets") {
    override fun handle() {
        DungeonInfo.uniqueRooms[room]?.let {
            if (it.foundSecrets < secrets) it.foundSecrets = secrets
        }
    }
}


