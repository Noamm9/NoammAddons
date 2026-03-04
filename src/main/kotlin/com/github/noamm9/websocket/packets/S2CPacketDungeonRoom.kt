package com.github.noamm9.websocket.packets

import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Room
import com.github.noamm9.utils.dungeons.map.core.Unknown
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.websocket.PacketRegistry

class S2CPacketDungeonRoom(
    val name: String,
    val x: Int, val z: Int,
    val col: Int, val row: Int,
    val core: Int, val isSeparator: Boolean
): PacketRegistry.WebSocketPacket("dungeonroom") {
    override fun handle() {
        ThreadUtils.scheduledTask {
            if (DungeonScanner.hasScanned) return@scheduledTask
            val idx = row * 11 + col
            val tile = DungeonInfo.dungeonList[idx]
            if (tile is Unknown || (tile as? Room)?.data?.name == "Unknown") {
                val data = ScanUtils.getRoomData(name) ?: return@scheduledTask
                DungeonInfo.dungeonList[idx] = Room(x, z, data).also {
                    it.isSeparator = isSeparator
                    it.core = core
                    it.addToUnique(row, col)
                }
            }
        }
    }
}