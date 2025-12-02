package noammaddons.websocket.packets

import noammaddons.NoammAddons
import noammaddons.utils.ChatUtils
import noammaddons.websocket.PacketRegistry

class S2CPacketPong(var username: String = ""): PacketRegistry.WebSocketPacket("pong") {
    override fun handle() {
        NoammAddons.mc.addScheduledTask {
            ChatUtils.modMessage("&bFound User: &d$username")
        }
    }
}