package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.websocket.PacketRegistry

class S2CPacketDungeonPrince: PacketRegistry.WebSocketPacket("dungeonprince") {
    override fun handle() {
        ThreadUtils.scheduledTask {
            ScoreCalculation.princeKilled = true
        }
    }
}