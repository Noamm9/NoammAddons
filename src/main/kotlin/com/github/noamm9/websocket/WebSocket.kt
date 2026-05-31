package com.github.noamm9.websocket

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.WebSocketEvent
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.network.WebUtils
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.*

object WebSocket {
    private val worker = run {
        val threadFactory = fun(it: Runnable) = Thread(it, "${NoammAddons.MOD_NAME}-WebSocket").apply { isDaemon = true }
        CoroutineScope(Executors.newSingleThreadExecutor(threadFactory).asCoroutineDispatcher() + SupervisorJob())
    }

    @Volatile private var session: DefaultClientWebSocketSession? = null
    private var socketJob: Job? = null

    fun init() {
        ThreadUtils.addShutdownHook(::shutdown)
        PacketRegistry.init()
        connect()
    }

    fun send(packet: Any) = worker.launch {
        val socket = session?.takeIf { it.isActive } ?: return@launch
        val json = GsonUtils.gson.toJsonTree(packet).asJsonObject
        val type = PacketRegistry.getType(packet)
        if (type != null) json.addProperty("type", type)
        socket.send(Frame.Text(json.toString()))
        ChatUtils.debug("ws", "[WS] sending $json")
    }

    private fun connect() {
        if (socketJob?.isActive == true) return

        socketJob = worker.launch {
            try {
                WebUtils.client.webSocket("wss://ws.noamm.org", {
                    timeout {
                        requestTimeoutMillis = 60_000
                        socketTimeoutMillis = 60_000
                    }
                }) {
                    mc.submit { EventBus.post(WebSocketEvent.Connect) }
                    session = this

                    for (frame in incoming) if (frame is Frame.Text) mc.submit {
                        EventBus.post(WebSocketEvent.Payload(frame.readText()))
                    }
                }
            }
            catch (e: Exception) {
                ChatUtils.debug("ws", "[WS] disconnected")
                NoammAddons.logger.info("WebSocket: Disconnected", e)
                mc.submit { EventBus.post(WebSocketEvent.Disconnect) }
            }
            finally {
                session = null
                socketJob = null
                ThreadUtils.setTimeout(30_000, ::connect)
            }
        }
    }

    private fun shutdown() = runBlocking {
        catch { session?.close() }
        catch { session?.cancel() }
        catch { socketJob?.cancelAndJoin() }
        worker.cancel()
    }
}