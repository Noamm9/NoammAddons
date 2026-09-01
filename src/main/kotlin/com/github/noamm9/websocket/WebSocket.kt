package com.github.noamm9.websocket

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.logger
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.event.impl.WebSocketEvent
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.network.ApiAuth
import com.github.noamm9.utils.network.WebUtils
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.parameter
import io.ktor.websocket.*
import kotlinx.coroutines.*

object WebSocket: ISelfInit {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("${NoammAddons.MOD_NAME}-WebSocket"))
    @Volatile private var session: WebSocketSession? = null
    @Volatile private var connecting = false

    override fun init() {
        register<GameStartEvent> {
            scope.launch {
                delay(5000)
                connect()
            }
        }
    }

    fun send(packet: Any) = scope.launch {
        val socket = session?.takeIf(WebSocketSession::isActive) ?: return@launch
        val json = GsonUtils.gson.toJsonTree(packet).asJsonObject
        val type = PacketRegistry.getType(packet)
        if (type != null) json.addProperty("type", type)
        socket.send(Frame.Text(json.toString()))
        ChatUtils.debug("ws", "[WS] sending $json")
    }

    suspend fun connect() {
        if (connecting) return
        connecting = true

        try {
            val token = ApiAuth.token ?: return logger.info("[Websocket] no auth token yet, waiting for auth")
            WebUtils.client.webSocket("wss://ws.noamm.org", {
                parameter("name", mc.user.name)
                parameter("token", token)
            }) {
                session = this

                for (frame in incoming) if (frame is Frame.Text) mc.submit {
                    EventBus.post(WebSocketEvent.Payload(frame.readText()))
                }
            }
        }
        catch (e: Throwable) {
            if (e is CancellationException) throw e
            ChatUtils.modMessage("[WebSocket] error: ${e.message}")
        }
        finally {
            session = null
            connecting = false
            scope.launch {
                delay(60_000L)
                connect()
            }
        }
    }
}