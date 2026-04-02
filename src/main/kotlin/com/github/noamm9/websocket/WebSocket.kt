package com.github.noamm9.websocket

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.debugFlags
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.JsonUtils
import com.google.gson.JsonParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object WebSocket {
    private val worker = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "${NoammAddons.MOD_NAME}-WebSocket").apply { isDaemon = true }
    }

    fun init() {
        PacketRegistry.init()
        NASocket.connect()
        Runtime.getRuntime().addShutdownHook(Thread { NASocket.close(1000, "client stopping") })
    }

    fun send(packet: Any) = worker.execute {
        if (! NASocket.isOpen) return@execute
        val raw = JsonUtils.gsonBuilder.toJson(packet)
        if (debugFlags.contains("ws")) ChatUtils.chat(raw)
        runCatching { NASocket.send(raw) }
    }

    private object NASocket: WebSocketClient(URI("wss://noamm.org")) {
        override fun onMessage(message: String) = worker.execute {
            runCatching {
                val json = JsonParser.parseString(message).takeIf { it.isJsonObject }?.asJsonObject ?: return@execute
                val type = json.get("type")?.asString.takeUnless { it.isNullOrBlank() } ?: return@execute
                val packetClass = PacketRegistry.getPacketClass(type) ?: return@execute
                val packet = JsonUtils.gsonBuilder.fromJson(message, packetClass)
                mc.execute { packet.handle() }
            }
        }

        init {
            connectionLostTimeout = 30
            isTcpNoDelay = true
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) = worker.execute { worker.schedule({ reconnect() }, 30, TimeUnit.SECONDS) }
        override fun onOpen(handshakedata: ServerHandshake) = worker.execute { NoammAddons.logger.info("WebSocket: Connected Successfully") }
        override fun onError(ex: Exception) = worker.execute { NoammAddons.logger.error("WebSocket: transport error", ex) }
    }
}