package com.github.noamm9.websocket

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.websocket.packets.C2SPacketServerHash
import com.google.gson.JsonParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.Executors

object WebSocket {
    private var socketClient: WebSocketClient? = null
    private var sendExecutor = Executors.newSingleThreadExecutor()

    var hash: String? = null
        set(value) {
            if (field == value) return
            field = value

            if (value != null) {
                send(C2SPacketServerHash(value.hashCode()))
            }
        }

    fun init() {
        PacketRegistry.init()

        runCatching {
            NoammAddons.logger.info("WebSocket: Initializing connection...")
            socketClient = NASocket()
            socketClient !!.connectionLostTimeout = 30
            socketClient !!.addHeader("User-Agent", NoammAddons.MOD_NAME)
            socketClient !!.connect()

            Runtime.getRuntime().addShutdownHook(Thread {
                socketClient?.close()
                sendExecutor.shutdownNow()
            })
        }.onFailure {
            NoammAddons.logger.error("Failed to Connect to Websocket")
            it.printStackTrace()
        }
    }

    fun send(packet: PacketRegistry.WebSocketPacket) = sendExecutor.execute {
        if (socketClient == null || ! socketClient !!.isOpen) return@execute
        runCatching {
            socketClient !!.send(JsonUtils.gsonBuilder.toJson(packet))
        }.onFailure {
            NoammAddons.logger.error("Failed to send packet: ${it.message}")
        }
    }

    private class NASocket: WebSocketClient(URI("wss://noamm.org")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            NoammAddons.logger.info("Connected to WebSocket")
            hash = LocationUtils.lobbyId
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

            sendExecutor.shutdownNow()
            sendExecutor = Executors.newSingleThreadExecutor()

            ThreadUtils.setTimeout(10_000) {
                NoammAddons.logger.info("Attempting auto-reconnect...")
                reconnect()
            }
        }

        override fun onError(ex: Exception?) {
            NoammAddons.logger.error("WebSocket Error: ${ex?.message}")
        }
    }
}