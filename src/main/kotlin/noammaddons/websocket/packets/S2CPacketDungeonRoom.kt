package noammaddons.websocket.packets

import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dmap.core.map.Room
import noammaddons.features.impl.dungeons.dmap.core.map.Unknown
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.utils.ScanUtils
import noammaddons.websocket.PacketRegistry

class S2CPacketDungeonRoom(
    val name: String,
    val x: Int, val z: Int,
    val col: Int, val row: Int,
    val core: Int, val isSeparator: Boolean
): PacketRegistry.WebSocketPacket("dungeonroom") {
    override fun handle() {
        mc.addScheduledTask {
            if (DungeonScanner.hasScanned) return@addScheduledTask
            val idx = row * 11 + col
            val tile = DungeonInfo.dungeonList[idx]
            if (tile is Unknown || (tile as? Room)?.data?.name == "Unknown") {
                val data = ScanUtils.getRoomData(name) ?: return@addScheduledTask
                DungeonInfo.dungeonList[idx] = Room(x, z, data).also {
                    it.isSeparator = isSeparator
                    it.core = core
                    it.addToUnique(row, col)
                }
            }
        }
    }
}