package com.github.noamm9.websocket

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import com.google.gson.JsonParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

object WebSocket {
    fun init() {
        PacketRegistry.init()
        Runtime.getRuntime().addShutdownHook(Thread { NASocket.close() })

        runCatching {
            NoammAddons.logger.info("WebSocket: Initializing connection...")
            NASocket.connect()
        }.onFailure {
            NoammAddons.logger.error("Failed to Connect to Websocket")
            it.printStackTrace()
        }
    }

    fun send(packet: PacketRegistry.WebSocketPacket) = runCatching {
        if (NASocket.isOpen) {
            val jsonObject = JsonUtils.gsonBuilder.toJsonTree(packet).asJsonObject
            jsonObject.addProperty("serverId", LocationUtils.serverId)
            val finalJson = JsonUtils.gsonBuilder.toJson(jsonObject)
            NASocket.send(finalJson)
        }
    }

    private object NASocket: WebSocketClient(URI("wss://noamm.org")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            NoammAddons.logger.info("Connected to WebSocket")
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
}