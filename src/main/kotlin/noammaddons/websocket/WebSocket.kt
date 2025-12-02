package noammaddons.websocket

import com.google.gson.JsonParser
import gg.essential.api.EssentialAPI
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.NoammAddons
import noammaddons.NoammAddons.Companion.MOD_NAME
import noammaddons.NoammAddons.Companion.MOD_VERSION
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.WorldLoadPostEvent
import noammaddons.utils.*
import noammaddons.websocket.packets.C2SPacketServerHash
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

object WebSocket {
    private var socketClient: WebSocketClient? = null
    private var receivedLocrawThisWorld = false
    private var locrawCountdown = - 1
    private var currentServer = 0

    fun init() {
        if (socketClient != null && socketClient !!.isOpen) return

        runCatching {
            socketClient = NASocket()
            socketClient !!.addHeader("User-Agent", "$MOD_NAME - $MOD_VERSION")
            socketClient !!.connect()

            EssentialAPI.getShutdownHookUtil().register {
                socketClient?.close()
            }
        }.onFailure { NoammAddons.Logger.error(it) }

        MinecraftForge.EVENT_BUS.register(WebSocket)
        MinecraftForge.EVENT_BUS.register(WebSocketTest)
        PacketRegistry.init()
    }

    fun send(packet: PacketRegistry.WebSocketPacket) {
        if (socketClient != null && socketClient !!.isOpen) ThreadUtils.runOnNewThread {
            val json = JsonUtils.gson.toJson(packet)
            socketClient !!.send(json)
        }
    }


    @SubscribeEvent
    fun onWorldLoad(event: WorldLoadPostEvent) {
        receivedLocrawThisWorld = false
        locrawCountdown = 60
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return

        if (locrawCountdown > 0) {
            locrawCountdown --

            if (locrawCountdown == 0) {
                if (! receivedLocrawThisWorld && LocationUtils.onHypixel) {
                    NoammAddons.Logger.info("Requesting Locraw...")
                    mc.thePlayer.sendChatMessage("/locraw")
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2) return
        val text = event.message.unformattedText

        if (text.startsWith("{") && text.contains("\"server\":") && text.endsWith("}")) {
            val regex = Regex("^\\{\"server\":\"(.+)\",\"gametype\":\"(.+)\",\"mode\":\"(.+)\",\"map\":\"(.+)\"\\}\$")
            val matchResult = regex.find(text)
            event.isCanceled = true
            receivedLocrawThisWorld = true
            locrawCountdown = - 1

            if (matchResult != null) {
                val (server, gametype, mode, map) = matchResult.destructured
                val newHash = (server + gametype + mode + map).hashCode()

                if (currentServer != newHash) {
                    currentServer = newHash
                    NoammAddons.Logger.info("Detected World Change. Hash: $currentServer")
                    send(C2SPacketServerHash(currentServer))
                }
            }
        }
    }

    private class NASocket: WebSocketClient(URI("wss://api.noammaddons.workers.dev")) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            NoammAddons.Logger.info("WebSocket Connected")
        }

        override fun onMessage(message: String?) {
            if (message == null) return
            try {
                val json = JsonParser().parse(message).asJsonObject.takeIf { it.has("type") } ?: return
                val type = json.get("type").asString
                val packetClass = PacketRegistry.getPacketClass(type)
                    ?: return NoammAddons.Logger.warn("Unknown packet type received: $type")

                val packet = JsonUtils.gson.fromJson(message, packetClass)
                packet.handle()
            }
            catch (e: Exception) {
                NoammAddons.Logger.error("Error handling packet: ${e.message}")
                e.printStackTrace()
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            NoammAddons.Logger.info("WebSocket Disconnected: $reason")
            socketClient = null
        }

        override fun onError(ex: Exception?) {
            NoammAddons.Logger.error("WebSocket Error: ${ex?.message}")
        }
    }
}

