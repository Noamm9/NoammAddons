package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.dungeons.map.core.RoomTile
import com.github.noamm9.utils.dungeons.map.core.Unknown
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.websocket.WebSocketPacket

class S2CPacketDungeonRoom(val name: String, val x: Int, val z: Int, val col: Int, val row: Int, val isSeparator: Boolean): WebSocketPacket {
    override fun handle() {
        if (DungeonScanner.hasScanned) return
        val tile = DungeonScanner.dungeonList[row * 11 + col]
        if (tile !is Unknown && (tile as? RoomTile)?.data?.isUnknown() != true) return
        val data = ScanUtils.getRoomData(name) ?: return

        DungeonScanner.dungeonList[row * 11 + col] = RoomTile(x, z, data).also {
            it.isSeparator = isSeparator
            it.addToUnique(row, col)
        }
    }
}