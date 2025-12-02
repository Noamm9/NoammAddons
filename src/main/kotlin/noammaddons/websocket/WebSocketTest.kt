package noammaddons.websocket

import gg.essential.universal.UChat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.MessageSentEvent
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum
import noammaddons.utils.ChatUtils
import noammaddons.utils.JsonUtils
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
                    JsonUtils.gson.toJson(S2CPacketM7Dragon(S2CPacketM7Dragon.DragonEvent.SPAWN, WitherDragonEnum.Orange))
                }.getOrNull().let { UChat.chat(it.toString()) }
            }

            "chat" -> {
                event.isCanceled = true
                if (args.isEmpty()) return
                S2CPacketChat(args.joinToString(" ")).let {
                    it.handle()
                    WebSocket.send(it)
                }
            }

            "info" -> {
            }
        }
    }
}