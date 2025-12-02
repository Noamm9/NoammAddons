package noammaddons.websocket.packets

import noammaddons.NoammAddons
import noammaddons.features.impl.dungeons.MimicDetector
import noammaddons.websocket.PacketRegistry

class S2CPacketDungeonPrince: PacketRegistry.WebSocketPacket("dungeonprince") {
    override fun handle() {
        NoammAddons.mc.addScheduledTask {
            MimicDetector.princeKilled.set(true)
        }
    }
}