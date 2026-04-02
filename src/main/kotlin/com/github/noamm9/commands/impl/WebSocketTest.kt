package com.github.noamm9.commands.impl

import com.github.noamm9.NoammAddons
import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.websocket.WebSocket
import com.github.noamm9.websocket.packets.S2CPacketChat
import com.mojang.brigadier.arguments.StringArgumentType

object WebSocketTest: BaseCommand("ws") {
    override fun CommandNodeBuilder.build() {
        literal("chat") {
            argument("message", StringArgumentType.greedyString()) {
                runs {
                    val message = StringArgumentType.getString(it, "message").addColor()
                    WebSocket.send(S2CPacketChat("§d${NoammAddons.mc.user.name}: §r$message").apply { handle() })
                }
            }

            runs {
                ChatUtils.modMessage("/ws chat <message>")
            }
        }

        literal("users") {
            runs {
                WebSocket.send(mapOf("type" to "check_users"))
            }
        }
    }
}