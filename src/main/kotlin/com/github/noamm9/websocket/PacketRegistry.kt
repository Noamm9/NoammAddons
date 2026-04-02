package com.github.noamm9.websocket

import com.github.noamm9.websocket.packets.*


object PacketRegistry {
    private val packets = HashMap<String, Class<out WebSocketPacket>>()

    fun init() {
        register<S2CPacketChat>("chat")
        register<S2CPacketDungeonRoom>("dungeonroom")
        register<S2CPacketDungeonDoor>("dungeondoor")
        register<S2CPacketDungeonMimic>("dungeonmimic")
        register<S2CPacketDungeonScore>("dungeonprince")
        register<S2CPacketRoomSecrets>("dungeonroomsecrets")
        register<S2CPacketM7Dragon>("m7dragon")
        register<S2CPacketSocketInfo>("socket_info")
    }

    fun getPacketClass(type: String): Class<out WebSocketPacket>? {
        return packets[type]
    }

    private inline fun <reified T: WebSocketPacket> register(type: String) {
        packets[type] = T::class.java
    }
}


