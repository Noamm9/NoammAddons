package com.github.noamm9.websocket

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.TabListUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.websocket.packets.C2SPacketTabListUpdate
import com.google.gson.JsonParser
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

object WebSocket {
    private var lastTablist = emptyList<String>()

    fun init() {
        PacketRegistry.init()

        runCatching {
            NoammAddons.logger.info("WebSocket: Initializing connection...")
            NASocket.connect()

            Runtime.getRuntime().addShutdownHook(Thread { NASocket.close() })
        }.onFailure {
            NoammAddons.logger.error("Failed to Connect to Websocket")
            it.printStackTrace()
        }

        EventBus.register<MainThreadPacketReceivedEvent.Post>(EventPriority.LOW) {
            if (! LocationUtils.inSkyblock) return@register
            if (event.packet !is ClientboundPlayerInfoUpdatePacket) return@register
            if (! event.packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) return@register
            sendTabList()
        }

        EventBus.register<WorldChangeEvent> { lastTablist = emptyList() }
    }

    fun send(packet: PacketRegistry.WebSocketPacket) = runCatching {
        if (NASocket.isOpen) NASocket.send(JsonUtils.gsonBuilder.toJson(packet))
    }

    private object NASocket: WebSocketClient(URI("wss://noamm.org")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            NoammAddons.logger.info("Connected to WebSocket")
            sendTabList()
        }

        override fun onMessage(message: String?) {
            if (message == null) return
            runCatching {
                val json = JsonParser.parseString(message).asJsonObject.takeIf { it.has("type") } ?: return
                val type = json.get("type").asString
                val packetClass = PacketRegistry.getPacketClass(type)
                    ?: return NoammAddons.logger.warn("Unknown packet type received: $type")

                JsonUtils.gsonBuilder.fromJson(message, packetClass).handle()
            }.onFailure {
                NoammAddons.logger.error("Error parsing packet: ${it.message}")
                it.printStackTrace()
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            NoammAddons.logger.info("WebSocket Disconnected: code: $code, remote: $remote, reason: $reason")

            ThreadUtils.setTimeout(10_000) {
                NoammAddons.logger.info("Attempting auto-reconnect...")
                reconnect()
            }
        }

        override fun onError(ex: Exception?) = NoammAddons.logger.error("WebSocket Error: ${ex?.message}")
    }

    private fun sendTabList() {
        val players = TabListUtils.getTabList()
            .mapNotNull { it.second.profile.name }
            .filterNot { it.matches("^![A-Z]-[a-z]$".toRegex()) }
            .filter { it != mc.user.name }
            .takeIf { it.isNotEmpty() && it != lastTablist } ?: return

        lastTablist = players
        send(C2SPacketTabListUpdate(mc.user.name, players))
    }
}