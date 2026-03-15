package com.github.noamm9.websocket.packets

import com.github.noamm9.websocket.PacketRegistry

class C2SPacketTabListUpdate(val username: String, val tabList: List<String>): PacketRegistry.WebSocketPacket("tablist_update") {
    override fun handle() {}
}