package noammaddons.websocket.packets

import gg.essential.universal.UChat
import noammaddons.NoammAddons.Companion.mc
import noammaddons.websocket.PacketRegistry

class S2CPacketSocketInfo(
    var connectedUsers: Int = 0,
    var usersInLobby: Int = 0,
    var lobby: String = ""
): PacketRegistry.WebSocketPacket("socket_info") {
    override fun handle() {
        mc.addScheduledTask {
            UChat.chat("§b§m--------------------------------")
            UChat.chat("§6§lWebSocket Stats")
            UChat.chat("")
            UChat.chat(" §fTotal Online: §a$connectedUsers")
            UChat.chat(" §fUsers in your World: §a$usersInLobby")
            UChat.chat(" §fCurrent Hash: §7$lobby")
            UChat.chat("§b§m--------------------------------")
        }
    }
}

