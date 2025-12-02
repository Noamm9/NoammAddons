package noammaddons.websocket.packets

import noammaddons.websocket.PacketRegistry

class C2SPacketServerHash(hash: Int): PacketRegistry.WebSocketPacket("server_hash") {
    override fun handle() {}
}