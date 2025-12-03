package noammaddons.websocket

import noammaddons.websocket.packets.*


object PacketRegistry {
    private val packets = HashMap<String, Class<out WebSocketPacket>>()

    fun init() = packets.apply {
        set("chat", S2CPacketChat::class.java)
        set("dungeonmimic", S2CPacketDungeonMimic::class.java)
        set("dungeonprince", S2CPacketDungeonPrince::class.java)
        set("ping", C2SPacketPing::class.java)
        set("pong", S2CPacketPong::class.java)
        set("m7dragon", S2CPacketM7Dragon::class.java)
        set("dungeonroomsecrets", S2CPacketRoomSecrets::class.java)
        set("dungeonroom", S2CPacketDungeonRoom::class.java)
        set("dungeondoor", S2CPacketDungeonDoor::class.java)
        set("socket_info", S2CPacketSocketInfo::class.java)
    }

    fun getPacketClass(type: String): Class<out WebSocketPacket>? {
        return packets[type]
    }

    abstract class WebSocketPacket(val type: String) {
        abstract fun handle()
    }
}


