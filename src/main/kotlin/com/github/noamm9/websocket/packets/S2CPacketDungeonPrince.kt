package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.websocket.WebSocketPacket

object S2CPacketDungeonPrince: WebSocketPacket {
    override fun handle() = ScoreCalculation::princeKilled.set(true)
}