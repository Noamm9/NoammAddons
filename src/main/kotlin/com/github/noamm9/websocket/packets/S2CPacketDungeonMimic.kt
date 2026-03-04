package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.websocket.PacketRegistry

class S2CPacketDungeonMimic: PacketRegistry.WebSocketPacket("dungeonmimic") {
    override fun handle() {
        ThreadUtils.scheduledTask {
            ScoreCalculation.mimicKilled = true
        }
    }
}