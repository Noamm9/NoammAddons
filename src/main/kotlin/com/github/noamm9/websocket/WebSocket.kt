package com.github.noamm9.websocket

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Room
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.LocrawListener
import com.github.noamm9.utils.network.WebUtils
import com.github.noamm9.websocket.packets.C2SPacketDungeonStart
import com.google.gson.JsonElement
import com.google.gson.JsonParser
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

        EventBus.register<DungeonEvent.RunStatedEvent> { sendDungeonInfo() }
        EventBus.register<WorldChangeEvent> { send(mapOf("type" to "reset")) }
    }

    fun send(packet: Any) = worker.launch {
        val socket = session?.takeIf { it.isActive } ?: return@launch
        val raw = JsonUtils.gsonBuilder.toJson(packet)
        ChatUtils.debug("ws", "[WS] sending $raw")
        socket.send(Frame.Text(raw))
    }

    fun sendDungeonInfo() = ThreadUtils.scheduledTaskServer(30) ws@{
        if (DungeonListener.dungeonTeammatesNoSelf.isEmpty()) return@ws
        val serverId = LocrawListener.server.ifEmpty { LocationUtils.serverId } ?: return@ws
        val floor = LocationUtils.dungeonFloor ?: return@ws
        val team = DungeonListener.dungeonTeammates.map { it.name }.ifEmpty { return@ws }
        val entrance = (DungeonInfo.dungeonList.find { (it as? Room)?.data?.type == RoomType.ENTRANCE } as? Room)?.getArrayPosition() ?: return@ws

        send(C2SPacketDungeonStart(serverId, floor, team, entrance))
    }


    private fun connect() {
        if (socketJob?.isActive == true) return

        socketJob = worker.launch {
            try {
                WebUtils.client.webSocket("wss://noamm.org", {
                    timeout {
                        requestTimeoutMillis = 60_000
                        socketTimeoutMillis = 60_000
                    }
                }) {
                    NoammAddons.logger.info("WebSocket: Connected Successfully")
                    ChatUtils.debug("ws", "[WS] Connected Successfully")
                    session = this

                    for (frame in incoming) if (frame is Frame.Text) handleMessage(frame.readText())
                }
            }
            catch (e: Exception) {
                ChatUtils.debug("ws", "[WS] disconnected")
                NoammAddons.logger.info("WebSocket: Disconnected", e)
            }
            finally {
                session = null
                socketJob = null
                ThreadUtils.setTimeout(30_000, ::connect)
            }
        }
    }

    private fun handleMessage(message: String) = catch {
        ChatUtils.debug("ws", "[WS] recived message $message")
        val json = JsonParser.parseString(message).takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@catch
        val type = json.get("type")?.asString?.takeUnless(String::isBlank) ?: return@catch
        val packetClass = PacketRegistry.getPacketClass(type) ?: return@catch
        val packet = JsonUtils.gsonBuilder.fromJson(message, packetClass)
        mc.submit(packet::handle)
    }

    private fun shutdown() = runBlocking {
        catch { session?.close() }
        catch { socketJob?.cancelAndJoin() }
        worker.cancel()
    }
}