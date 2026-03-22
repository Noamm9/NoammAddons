package com.github.noamm9.commands.impl

import com.github.noamm9.NoammAddons
import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.impl.floor7.dragons.WitherDragonEnum
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.websocket.WebSocket
import com.github.noamm9.websocket.packets.C2SPacketCheckUsers
import com.github.noamm9.websocket.packets.C2SPacketPing
import com.github.noamm9.websocket.packets.S2CPacketChat
import com.github.noamm9.websocket.packets.S2CPacketM7Dragon
import com.mojang.brigadier.arguments.StringArgumentType

object WebSocketTest: BaseCommand("ws") {
    override fun CommandNodeBuilder.build() {
        literal("ping") {
            runs {
                ChatUtils.modMessage("Pinging all NA users in server...")
                WebSocket.send(C2SPacketPing())
            }
        }

        literal("dragon") {
            runs {
                runCatching {
                    JsonUtils.gsonBuilder.toJson(S2CPacketM7Dragon(S2CPacketM7Dragon.DragonEvent.SPAWN, WitherDragonEnum.Orange))
                }.getOrNull().let { ChatUtils.chat(it.toString()) }
            }
        }

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
                WebSocket.send(C2SPacketCheckUsers())
            }
        }
    }
}