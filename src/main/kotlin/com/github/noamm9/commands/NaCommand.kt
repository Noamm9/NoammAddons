package com.github.noamm9.commands

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.NoammDebugFlagEvent
import com.github.noamm9.features.impl.dev.UpdateChecker
import com.github.noamm9.features.impl.dungeon.LeapMenu
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.hud.HudEditorScreen
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.network.NoammAPI
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object NaCommand: ICommandProvider {
    override fun CommandBuilder.command() {
        setName("na", "noamm", "noammaddons")
        description("opens the Config GUI")
        runs { GuiUtils.setScreen(ClickGuiScreen()) }

        literal("help") {
            runs {
                val helpMenu = StringBuilder()
                this@command.helpEntries().forEach { (usage, desc) ->
                    helpMenu.append("§e/$usage §7- $desc\n")
                }
                ChatUtils.chat(helpMenu.toString().trim())
            }
        }

        literal("discord") {
            description("Opens the link to the Discord server")
            runs {
                Utils.openDiscordLink()
            }
        }

        literal("hud") {
            description("HUD editor")
            runs { GuiUtils.setScreen(HudEditorScreen()) }
        }

        literal("update") {
            description("Checks for an update of the mod")
            runs { ThreadUtils.async { UpdateChecker.runCheck(true) } }
        }

        literal("ping") {
            description("Shows your ping in chat")
            runs {
                ChatUtils.modMessage("§aPing: §f${ServerUtils.averagePing}ms")
            }
        }

        literal("tps") {
            description("Shows the server's tps in chat")
            runs {
                ChatUtils.modMessage("§aTPS: §f${ServerUtils.tps}")
            }
        }

        literal("debug") {
            description("Debug flags")
            runs {
                ChatUtils.modMessage("§7Flags: §f${NoammAddons.debugFlags.joinToString(", ")}")
            }

            argument("flag", StringArgumentType.greedyString()) {
                suggests { NoammAddons.availableDebugFlags }
                runs { ctx ->
                    StringArgumentType.getString(ctx, "flag").split(Regex("\\s+")).forEach { flag ->
                        val added = if (NoammAddons.debugFlags.remove(flag)) false else NoammAddons.debugFlags.add(flag)
                        ChatUtils.modMessage(if (added) "§aAdded debug flag: §b$flag" else "§cRemoved debug flag: §b$flag")
                        EventBus.post(if (added) NoammDebugFlagEvent.Add(flag) else NoammDebugFlagEvent.Remove(flag))
                    }
                }
            }
        }

        literal("sim") {
            runs {
                ChatUtils.modMessage("§cInvalid Usage: §f/na sim <message>")
            }

            argument("message", StringArgumentType.greedyString()) {
                description("Simulate chat message")
                runs { ctx ->
                    val msg = StringArgumentType.getString(ctx, "message").addColor()
                    ChatUtils.modMessage(msg)
                    EventBus.post(ChatMessageEvent(Component.literal(msg)))
                }
            }
        }

        literal("leaporder") {
            description("Configure custom leap sorting")
            val partyMembersSuggestion = { PartyUtils.members.map(String::lowercase) }
            argument("sorting", StringArgumentType.word()) {
                suggests { listOf("name", "class") }

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
        }

        literal("rtca") {
            description("Shows the runs needed for each class to hit class average 50")
            runs { sendRtca() }
            argument("name", StringArgumentType.word()) {
                runs {
                    sendRtca(StringArgumentType.getString(it, "name"))
                }
            }
        }

        //#if CHEAT
        literal("swapmask") {
            description("Equips either Bonzo Mask or Spirit Mask")
            runs {
                NoammAddons.scope.launch {
                    PlayerUtils.changeMaskAction()
                }
            }
        }

        literal("rodswap") {
            description("Automatically rodswaps for you")
            runs {
                NoammAddons.scope.launch {
                    PlayerUtils.rodSwap()
                }
            }
        }

        literal("swapto") {
            runs { ChatUtils.modMessage("missing skyblock id argument. /na swapto <ItemID>") }
            argument("skyblock id", StringArgumentType.word()) {
                description("Automatically equips the item in the EQ menu")
                runs {
                    NoammAddons.scope.launch {
                        val inv = NoammAddons.mc.player?.inventory?.nonEquipmentItems ?: return@launch
                        val item = StringArgumentType.getString(it, "skyblock id")
                        if (inv.none { it.skyblockId == item }) return@launch ChatUtils.modMessage("$item not found in inventory")
                        PlayerUtils.quickSwapAction(item)
                    }
                }
            }
        }

        literal("leap") {
            argument("class", StringArgumentType.word()) {
                suggests { DungeonClass.entries.filterNot { it == DungeonClass.Empty }.map { it.name } }
                description("Automatically leaps to the selected class")
                runs { ctx ->
                    val clazz = StringArgumentType.getString(ctx, "class")
                    val player = DungeonListener.dungeonTeammatesNoSelf.find { it.clazz.name == clazz } ?: return@runs ChatUtils.modMessage("leap target not found")
                    NoammAddons.scope.launch { PlayerUtils.leapAction(player) }
                }
            }
        }
        //#endif
    }

    private fun setLeapOrder(ctx: CommandContext<FabricClientCommandSource>, count: Int) {
        val sortingType = StringArgumentType.getString(ctx, "sorting").lowercase()
        if (sortingType != "name" && sortingType != "class") return ChatUtils.modMessage("§cInvalid sorting type! Use 'name' or 'class'")

        val validPlayers = mutableListOf<String>()
        for (i in 1 .. count) {
            val inputName = StringArgumentType.getString(ctx, "player$i")
            validPlayers.add(inputName.lowercase())
        }

        LeapMenu.customLeapOrder = validPlayers
        LeapMenu.customLeapType = sortingType
        ChatUtils.modMessage("§aCustom leap order set to: §f$sortingType §awith players: §f${validPlayers.joinToString(", ")}")
    }

    private fun sendRtca(name: String = NoammAddons.mc.user.name) = NoammAddons.scope.launch(Dispatchers.IO) {
        NoammAPI.getRtca(name).onSuccess {
            ChatUtils.modMessage("${it.name} is ${it.runs} M7 runs away from ca50 (${formatClassRuns(it.classes)})")
        }.onFailure {
            ChatUtils.modMessage("An error occurred meow! (${it.message})")
        }
    }

    private fun formatClassRuns(runs: Map<String, Int>): String {
        return runs.filterValues { it > 0 }.entries.joinToString(" | ") { (name, runs) ->
            "${name.take(4).uppercaseFirst()} $runs"
        }
    }
}