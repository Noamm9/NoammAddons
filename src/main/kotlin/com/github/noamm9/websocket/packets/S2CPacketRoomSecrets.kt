package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.websocket.PacketRegistry

class S2CPacketRoomSecrets(val room: String, val secrets: Int): PacketRegistry.WebSocketPacket("dungeonroomsecrets") {
    override fun handle() {
        ThreadUtils.scheduledTask {
            DungeonInfo.uniqueRooms[room]?.let {
                if (it.foundSecrets < secrets) {
                    it.foundSecrets = secrets
                }
            }
        }
    }
}


