package noammaddons.websocket.packets

import net.minecraft.util.ChatComponentText
import noammaddons.NoammAddons.Companion.mc
import noammaddons.websocket.PacketRegistry

class S2CPacketChat(val message: String): PacketRegistry.WebSocketPacket("chat") {
    override fun handle() {
        mc.addScheduledTask {
            mc.thePlayer?.addChatMessage(
                ChatComponentText("§b[WS]§r $message")
            )
        }
    }
}