package com.github.noamm9.websocket

import com.github.noamm9.websocket.packets.*

object PacketRegistry {
    val packets = mutableMapOf<String, Class<out WebSocketPacket>>()

    fun init() {
        register<S2CPacketChat>("chat")
        register<S2CPacketDungeonDoor>("dungeondoor")
        register<S2CPacketDungeonMimic>("dungeonmimic")
        register<S2CPacketDungeonPrince>("dungeonprince")
        register<S2CPacketDungeonRoom>("dungeonroom")
        register<S2CPacketRoomSecrets>("dungeonroomsecrets")
        register<S2CPacketM7Dragon>("m7dragon")
        register<S2CPacketSocketInfo>("socket_info")
    }

    fun getClass(type: String) = packets[type]
    fun getType(obj: Any) = packets.entries.find { it.value == obj::class.java }?.key

    private inline fun <reified T: WebSocketPacket> register(type: String) = packets.set(type, T::class.java)
}