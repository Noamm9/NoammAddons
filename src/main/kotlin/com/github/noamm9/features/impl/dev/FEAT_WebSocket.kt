package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.WebSocketEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Room
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.LocrawListener
import com.github.noamm9.websocket.PacketRegistry
import com.github.noamm9.websocket.WebSocket.send
import com.github.noamm9.websocket.packets.C2SPacketDungeonStart
import com.google.gson.JsonElement
import com.google.gson.JsonParser

object FEAT_WebSocket: Feature(name = "WebSocket", toggled = true) {
    override fun toggle() = Unit

    override fun init() {
        register<WebSocketEvent.Connect> {
            NoammAddons.logger.debug("WebSocket: Connected Successfully")
            ChatUtils.debug("ws", "[WS] Connected Successfully")
        }

        register<WebSocketEvent.Payload> {
            ChatUtils.debug("ws", "[Payload] Payload: ${event.message}")
            val json = JsonParser.parseString(event.message).takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@register
            val type = json.get("type").asString.takeUnless(String::isBlank) ?: return@register
            val packetClass = PacketRegistry.getClass(type) ?: return@register
            GsonUtils.gson.fromJson(json, packetClass).handle()
        }

        EventBus.register<DungeonEvent.RunStatedEvent> { sendDungeonInfo() }
        EventBus.register<DungeonEvent.RunEndedEvent> { send(mapOf("type" to "dungeon_end")) }
        EventBus.register<WorldChangeEvent> { send(mapOf("type" to "reset")) }
    }

    fun sendDungeonInfo() = ThreadUtils.scheduledTaskServer(30) ws@{
        if (DungeonListener.dungeonTeammatesNoSelf.isEmpty()) return@ws
        val serverId = LocrawListener.server.ifEmpty { LocationUtils.serverId } ?: return@ws
        val floor = LocationUtils.dungeonFloor ?: return@ws
        val team = DungeonListener.dungeonTeammates.map { it.name }.ifEmpty { return@ws }
        val entrance = (DungeonInfo.dungeonList.find { (it as? Room)?.data?.type == RoomType.ENTRANCE } as? Room)?.getArrayPosition() ?: return@ws

        send(C2SPacketDungeonStart(serverId, floor, team, entrance))
    }
}