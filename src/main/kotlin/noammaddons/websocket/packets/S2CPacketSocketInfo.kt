package noammaddons.websocket.packets

import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import noammaddons.websocket.PacketRegistry

class S2CPacketSocketInfo(
    var connectedUsers: Int = 0,
    var usersInLobby: Int = 0,
    var lobby: String = ""
): PacketRegistry.WebSocketPacket("socket_info") {

    override fun handle() {
        val mc = Minecraft.getMinecraft()

        mc.addScheduledTask {
            fun msg(text: String) = mc.thePlayer.addChatMessage(ChatComponentText(text))

            msg("§b§m--------------------------------")
            msg("§6§lWebSocket Stats")
            msg("")
            msg(" §fTotal Online: §a$connectedUsers")
            msg(" §fUsers in your World: §a$usersInLobby")
            msg(" §fCurrent Hash: §7$lobby")
            msg("§b§m--------------------------------")
        }
    }
}

