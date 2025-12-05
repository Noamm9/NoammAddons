package noammaddons.websocket

import gg.essential.universal.UChat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.MessageSentEvent
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum
import noammaddons.utils.*
import noammaddons.websocket.packets.*

object WebSocketTest {
    @SubscribeEvent
    fun onMessage(event: MessageSentEvent) {
        if (! event.message.startsWith("/ws ")) return
        val parts = event.message.substring(4).split(" ")
        val command = parts.firstOrNull()?.lowercase() ?: return
        val args = parts.drop(1)

        when (command) {
            "ping" -> {
                event.isCanceled = true
                if (args.isNotEmpty()) return
                ChatUtils.modMessage("Pinging all NA users in server...")
                WebSocket.send(C2SPacketPing())
            }

            "dragon" -> {
                event.isCanceled = true
                if (args.isNotEmpty()) return
                runCatching {
                    JsonUtils.gsonBuilder.toJson(S2CPacketM7Dragon(S2CPacketM7Dragon.DragonEvent.SPAWN, WitherDragonEnum.Orange))
                }.getOrNull().let { UChat.chat(it.toString()) }
            }

            "chat" -> {
                event.isCanceled = true
                if (args.isEmpty()) return
                S2CPacketChat("§d${mc.session.username}: §r${args.joinToString(" ")}").let {
                    WebSocket.send(it)
                    it.handle()
                }
            }

            "users" -> {
                event.isCanceled = true
                WebSocket.send(C2SPacketCheckUsers())
            }

            "rot" -> {
                event.isCanceled = true
                ScanUtils.getEntityRoom(mc.thePlayer)?.rotation = listOf(0, 90, 180, 270).random()
            }
        }
    }
}