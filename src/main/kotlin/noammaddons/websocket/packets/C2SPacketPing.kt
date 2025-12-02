package noammaddons.websocket.packets

import noammaddons.NoammAddons
import noammaddons.websocket.PacketRegistry
import noammaddons.websocket.WebSocket

class C2SPacketPing: PacketRegistry.WebSocketPacket("ping") {
    override fun handle() {
        val myName = NoammAddons.mc.session.username ?: return
        WebSocket.send(S2CPacketPong(myName))
    }
}