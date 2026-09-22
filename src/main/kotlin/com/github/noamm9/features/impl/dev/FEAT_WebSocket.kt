package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.MessageSentEvent
import com.github.noamm9.event.impl.WebSocketEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.RoomTile
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.LocrawListener
import com.github.noamm9.websocket.PacketRegistry
import com.github.noamm9.websocket.WebSocket.send
import com.github.noamm9.websocket.packets.C2SPacketDungeonStart
import com.github.noamm9.websocket.packets.S2CPacketChat
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mojang.brigadier.arguments.StringArgumentType

object FEAT_WebSocket: Feature(name = "WebSocket", toggled = true), ICommandProvider {
    private val mutedPartyChat by ToggleSetting("Muted Party Chat", true).withDescription("Sends party messages through the WebSocket when Hypixel chat is muted during a dungeon.")
    private val partyChatPrefixes = listOf("pc ", "party chat ", "p chat ")
    private var pendingChatMessage: String? = null
    private var pendingChatMessageAt = 0L

    override fun toggle() = Unit

    override fun init() {
        register<WebSocketEvent.Connect> {
            NoammAddons.logger.debug("WebSocket: Connected Successfully")
            ChatUtils.debug("ws", "[WS] Connected Successfully")
        }

        register<WebSocketEvent.Payload> {
            ChatUtils.debug("ws", "[WS] Received payload: ${event.message}")
            val json = JsonParser.parseString(event.message).takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@register
            val type = json.get("type").asString.takeUnless(String::isBlank) ?: return@register
            val packetClass = PacketRegistry.getClass(type) ?: return@register
            GsonUtils.gson.fromJson(json, packetClass).handle()
        }

        register<DungeonEvent.RunStatedEvent> { sendDungeonInfo() }
        register<DungeonEvent.RunEndedEvent> { send(mapOf("type" to "dungeon_end")) }
        register<WorldChangeEvent>(EventPriority.HIGHEST) {
            pendingChatMessage = null
            if (LocationUtils.inDungeon) send(mapOf("type" to "reset"))
        }
        register<MessageSentEvent>(EventPriority.LOWEST) {
            if (! mutedPartyChat.value || ! LocationUtils.inDungeon) return@register
            pendingChatMessage = event.message
            pendingChatMessageAt = System.currentTimeMillis()
        }
        register<ChatMessageEvent> {
            if (! mutedPartyChat.value) return@register
            if (! event.unformattedText.startsWith("You are currently muted")) return@register
            if (! LocationUtils.inDungeon) return@register

            val pending = pendingChatMessage?.takeIf { System.currentTimeMillis() - pendingChatMessageAt < 5000L } ?: return@register
            pendingChatMessage = null
            event.isCanceled = true
            val prefix = partyChatPrefixes.find { pending.startsWith(it, ignoreCase = true) }
            val message = if (prefix == null) pending else pending.drop(prefix.length)
            sendChat(message)
        }
    }

    override fun CommandBuilder.command() {
        setName("ws")

        literal("users") {
            runs {
                send(mapOf("type" to "check_users"))
            }
        }

        literal("chat") {
            argument("message", StringArgumentType.greedyString()) {
                runs {
                    sendChat(StringArgumentType.getString(it, "message"))
                }
            }

            runs {
                ChatUtils.modMessage("/ws chat <message>")
            }
        }
    }

    private fun sendChat(message: String) {
        val packet = S2CPacketChat("§d${NoammAddons.mc.user.name}: §r${message.addColor()}")
        send(packet.apply(S2CPacketChat::handle))
    }

    fun sendDungeonInfo() = ThreadUtils.scheduledTaskServer(30) ws@{
        if (DungeonListener.dungeonTeammatesNoSelf.isEmpty()) return@ws
        val serverId = LocrawListener.server.ifEmpty { LocationUtils.serverId } ?: return@ws
        val floor = LocationUtils.dungeonFloor ?: return@ws
        val team = DungeonListener.dungeonTeammates.map { it.name }.ifEmpty { return@ws }
        val entrance = (DungeonScanner.dungeonList.find { (it as? RoomTile)?.data?.type == RoomType.ENTRANCE } as? RoomTile)?.getGridPos() ?: return@ws

        send(C2SPacketDungeonStart(serverId, floor, team, entrance.run { second to first }))
    }
}