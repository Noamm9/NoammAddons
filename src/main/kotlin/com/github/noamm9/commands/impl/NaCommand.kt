package com.github.noamm9.commands.impl

import com.github.noamm9.NoammAddons.debugFlags
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.NoammAddons.screen
import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.features.impl.dungeon.LeapMenu
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.hud.HudEditorScreen
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object NaCommand: BaseCommand("na") {
    private val commands = mapOf(
        "/na" to "Config GUI",
        "/na hud" to "HUD editor",
        "/na discord" to "Opens the link to the Discord server",
        "/na debug" to "Debug flags",
        "/na sim" to "Simulate chat message",
        "/na leaporder" to "Configure custom leap sorting",
        "/na ping" to "Shows your ping in chat",
        //#if CHEAT
        "/na swapmask" to "Equips either Bonzo Mask or Spirit Mask",
        "/na rodswap" to "Automatically rodswaps for you",
        "/na leap <class>" to "Automatically leaps to the selected class"
        //#endif
    )

    override fun CommandNodeBuilder.build() {
        runs { screen = ClickGuiScreen }

        literal("ping") {
            runs {
                ChatUtils.modMessage("§aPing: §f${ServerUtils.averagePing}ms")
            }
        }

        literal("discord") {
            runs {
                Utils.openDiscordLink()
            }
        }

        literal("help") {
            runs {
                val helpMenu = StringBuilder("§6§lNoammAddons§r\n")
                commands.forEach { (cmd, desc) -> helpMenu.append("§e$cmd §7- $desc\n") }
                ChatUtils.chat(helpMenu.toString().trim())
            }
        }

        literal("hud") {
            runs { screen = HudEditorScreen }
        }

        literal("debug") {
            runs {
                ChatUtils.modMessage("§7Flags: §f${debugFlags.joinToString(", ")}")
            }

            argument("flag", StringArgumentType.word()) {
                runs { ctx ->
                    val flag = StringArgumentType.getString(ctx, "flag")
                    if (debugFlags.remove(flag)) ChatUtils.modMessage("§cRemoved debug flag: §b$flag")
                    else {
                        debugFlags.add(flag)
                        ChatUtils.modMessage("§aAdded debug flag: §b$flag")
                    }
                }
            }
        }

        literal("sim") {
            runs {
                ChatUtils.modMessage("§cInvalid Usage: §f/na sim <message>")
            }

            argument("message", StringArgumentType.greedyString()) {
                runs { ctx ->
                    val msg = StringArgumentType.getString(ctx, "message").addColor()
                    ChatUtils.modMessage(msg)
                    EventBus.post(ChatMessageEvent(Component.literal(msg)))
                }
            }
        }

        literal("leaporder") {
            argument("player1", StringArgumentType.word()) {
                suggests(partyMembersSuggestion)
                runs { ctx -> setLeapOrder(ctx, 1) }

                argument("player2", StringArgumentType.word()) {
                    suggests(partyMembersSuggestion)
                    runs { ctx -> setLeapOrder(ctx, 2) }

                    argument("player3", StringArgumentType.word()) {
                        suggests(partyMembersSuggestion)
                        runs { ctx -> setLeapOrder(ctx, 3) }

                        argument("player4", StringArgumentType.word()) {
                            suggests(partyMembersSuggestion)
                            runs { ctx -> setLeapOrder(ctx, 4) }
                        }
                    }
                }
            }
        }

        //#if CHEAT
        literal("swapmask") {
            runs {
                scope.launch {
                    PlayerUtils.changeMaskAction()
                }
            }
        }

        literal("rodswap") {
            runs {
                scope.launch {
                    PlayerUtils.rodSwap()
                }
            }
        }

        literal("leap") {
            argument("class", StringArgumentType.word()) {
                suggests { DungeonClass.entries.filterNot { it == DungeonClass.Empty }.map { it.name } }
                runs { ctx ->
                    val clazz = StringArgumentType.getString(ctx, "class")
                    val player = DungeonListener.dungeonTeammatesNoSelf.find { it.clazz.name == clazz } ?: return@runs ChatUtils.modMessage("leap target not found")
                    scope.launch { PlayerUtils.leapAction(player) }
                }
            }
        }
        //#endif
    }

    private val partyMembersSuggestion = { PartyUtils.members.map { it.lowercase() } }

    private fun setLeapOrder(ctx: CommandContext<FabricClientCommandSource>, count: Int) {
        val validPlayers = mutableListOf<String>()

        for (i in 1 .. count) {
            val inputName = StringArgumentType.getString(ctx, "player$i")
            validPlayers.add(inputName.lowercase())
        }

        LeapMenu.customLeapOrder = validPlayers
        ChatUtils.modMessage("§aCustom leap order set to: §f${validPlayers.joinToString(", ")}")
    }
}

