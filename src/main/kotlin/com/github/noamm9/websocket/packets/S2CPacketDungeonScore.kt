package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.websocket.WebSocketPacket

class S2CPacketDungeonScore: WebSocketPacket("dungeonprince") {
    override fun handle() {
        ScoreCalculation.princeKilled = true
    }
}