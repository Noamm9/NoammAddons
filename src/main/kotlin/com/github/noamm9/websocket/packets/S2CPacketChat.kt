package com.github.noamm9.websocket.packets

//? if <26.2 {
/*import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ChatMessageEvent*/
//? }
import com.github.noamm9.utils.ChatUtils
//? if <26.2 {
/*import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.location.LocationUtils*/
//? }
import com.github.noamm9.websocket.WebSocketPacket
//? if <26.2 {
/*import net.minecraft.network.chat.Component*/
//? }

class S2CPacketChat(val message: String): WebSocketPacket {
    //? if <26.2 {
    /*override fun handle() {
        ChatUtils.chat("§b[WS]§r $message")
        if (! LocationUtils.inDungeon) return
        val text = message.removeFormatting()
        if (PartyUtils.members.none { it.equals(text.substringBefore(":"), ignoreCase = true) }) return
        EventBus.post(ChatMessageEvent(Component.literal("Party > $text")))
    }*/
    //? } else {
    override fun handle() = ChatUtils.chat("§b[WS]§r $message")
    //? }
}