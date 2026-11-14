package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.handlers.ScoreTracker
import com.github.noamm9.websocket.WebSocketPacket
import java.util.UUID

class S2CPacketDungeonBat(val uuid: UUID): WebSocketPacket {
    override fun handle() {
        ScoreTracker.registerBatKill(uuid)
    }
}