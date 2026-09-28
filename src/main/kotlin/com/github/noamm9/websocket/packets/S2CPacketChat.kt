package com.github.noamm9.websocket.packets

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.websocket.WebSocketPacket
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

class S2CPacketChat(val message: String): WebSocketPacket {
    override fun handle() {
        ChatUtils.chat("§b[WS]§r $message")
        if (! LocationUtils.onHypixel || ! LocationUtils.inDungeon) return
        val text = message.removeFormatting()
        val sender = chatPattern.matchEntire(text)?.groupValues?.get(1) ?: return
        if (PartyUtils.members.none { it.equals(sender, ignoreCase = true) }) return

        val connection = mc.connection ?: return
        val packet = ClientboundSystemChatPacket(Component.literal("Party > $text"), false)
        val wasHidden = hiddenPartyMessage.get()
        hiddenPartyMessage.set(true)
        try {
            if (! EventBus.post(MainThreadPacketReceivedEvent.Pre(packet))) {
                packet.handle(connection)
                EventBus.post(MainThreadPacketReceivedEvent.Post(packet))
            }
        }
        finally {
            if (wasHidden) hiddenPartyMessage.set(true) else hiddenPartyMessage.remove()
        }
    }

    companion object {
        private val chatPattern = Regex("^([A-Za-z0-9_]{1,16}): (.+)$")
        private val hiddenPartyMessage = ThreadLocal.withInitial { false }

        @JvmStatic fun isHiddenPartyMessage(): Boolean = hiddenPartyMessage.get()
    }
}