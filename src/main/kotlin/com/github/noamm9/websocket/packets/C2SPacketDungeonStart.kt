package com.github.noamm9.websocket.packets

import com.github.noamm9.websocket.WebSocketPacket

class C2SPacketDungeonStart(
    val serverId: String,
    val floor: String,
    val members: List<String>,
    val entrance: Pair<Int, Int>
): WebSocketPacket("dungeon_start") {
    override fun handle() {}
}