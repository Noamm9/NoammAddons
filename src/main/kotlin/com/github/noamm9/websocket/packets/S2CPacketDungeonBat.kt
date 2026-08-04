package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.websocket.WebSocketPacket

object S2CPacketDungeonBat: WebSocketPacket {
    override fun handle() = ScoreCalculation::batKilled.set(true)
}
