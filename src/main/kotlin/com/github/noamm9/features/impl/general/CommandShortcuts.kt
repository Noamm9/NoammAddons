package com.github.noamm9.features.impl.general

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket

object CommandShortcuts: Feature("Create your own command shortcuts") {
    var shortcuts by PogObject("commandShortcuts", linkedMapOf<String, String>())
    private var redirecting = false

    override fun init() {
        register<PacketEvent.Sent> {
            if (redirecting) return@register
            val packet = event.packet as? ServerboundChatCommandPacket ?: return@register
            val args = packet.command.split(" ", limit = 2)
            val replacement = shortcuts[args[0].lowercase()] ?: return@register
            val connection = mc.player?.connection ?: return@register
            event.isCanceled = true

            val message = if (args.size > 1) "$replacement ${args[1]}" else replacement
            redirecting = true
            try {
                if (message.startsWith("/")) connection.sendCommand(message.removePrefix("/"))
                else connection.sendChat(message)
            }
            finally {
                redirecting = false
            }
        }
    }
}
