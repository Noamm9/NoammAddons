package noammaddons.websocket.packets

import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.websocket.PacketRegistry

class S2CPacketRoomSecrets(val room: String, val secrets: Int): PacketRegistry.WebSocketPacket("dungeonroomsecrets") {
    override fun handle() {
        mc.addScheduledTask {
            DungeonInfo.uniqueRooms[room]?.let {
                if (it.foundSecrets < secrets) {
                    it.foundSecrets = secrets
                }
            }
        }
    }
}


