package noammaddons.websocket.packets

import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.websocket.PacketRegistry

class S2CPacketDungeonDoor(val x: Int, val z: Int, val col: Int, val row: Int, val doorType: DoorType): PacketRegistry.WebSocketPacket("dungeondoor") {
    override fun handle() {
        mc.addScheduledTask {
            val idx = row * 11 + col
            if (DungeonInfo.dungeonList[idx] is Unknown) {
                DungeonInfo.dungeonList[idx] = Door(x, z, doorType)
            }
        }
    }
}