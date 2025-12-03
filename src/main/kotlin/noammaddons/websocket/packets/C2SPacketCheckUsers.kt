package noammaddons.websocket.packets

import noammaddons.websocket.PacketRegistry

class C2SPacketCheckUsers: PacketRegistry.WebSocketPacket("check_users") {
    override fun handle() {}
}