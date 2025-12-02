package noammaddons.websocket.packets

import noammaddons.NoammAddons
import noammaddons.features.impl.dungeons.MimicDetector
import noammaddons.websocket.PacketRegistry

class S2CPacketDungeonMimic: PacketRegistry.WebSocketPacket("dungeonmimic") {
    override fun handle() {
        NoammAddons.mc.addScheduledTask {
            MimicDetector.mimicKilled.set(true)
        }
    }
}