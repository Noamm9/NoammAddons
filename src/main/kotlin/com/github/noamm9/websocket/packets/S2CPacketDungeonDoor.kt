package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Door
import com.github.noamm9.utils.dungeons.map.core.DoorType
import com.github.noamm9.utils.dungeons.map.core.Unknown
import com.github.noamm9.websocket.WebSocketPacket

class S2CPacketDungeonDoor(val x: Int, val z: Int, val col: Int, val row: Int, val doorType: DoorType): WebSocketPacket("dungeondoor") {
    override fun handle() {
        val idx = row * 11 + col
        if (DungeonInfo.dungeonList[idx] !is Unknown) return
        DungeonInfo.dungeonList[idx] = Door(x, z, doorType)
    }
}