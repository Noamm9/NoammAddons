package com.github.noamm9.features.impl.general

import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.config.PogObject
import com.github.noamm9.config.types.MultiCheckboxSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.addColor
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PartyUtils.isLeader
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.location.LocationUtils
import com.mojang.brigadier.arguments.StringArgumentType
import gg.essential.universal.USound
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt

object PartyHelper: Feature("Party commands and reformatting."), ICommandProvider {
    private val partyCommands by ToggleSetting("Party Commands", true).section("Party Commands")
    private val partyLeaderCheck by ToggleSetting("Leader Only", false).showIf { partyCommands.value }
    private val commands by MultiCheckboxSetting("Enabled Commands", mutableMapOf(
        "!w" to true, "!f" to true, "!m" to true, "!inv" to true,
        "!kick" to true, "!dt" to true, "!ping" to true, "!tps" to true, "!fps" to true,
        "!pt" to true, "!ai" to true, "!coords" to true, "!gay" to true
    )).showIf { partyCommands.value }

    private val partyAddons by ToggleSetting("Reformat Party List", true).section("Party Addons")

    private val playerBlacklist = PogObject("party_command_blacklist", mutableSetOf<String>())
    private val party = mutableListOf<PartyMember>()
    val downtimeList = mutableMapOf<String, String>()
    private var awaitingDelimiter = 0

    private val partyStartPattern = Regex("^Party Members \\((\\d+)\\)$")
    private val playerPattern = Regex("(?<rank>.*?)(?<name>\\w+) ?§(?<status>[ac]) ?● ?")
    private val partyCommandRegex = Regex("^Party > (?:\\[[^]]+] )?([^:]+): ([!?.\\-@#`/])(.+)$")

    override fun CommandBuilder.command() {
        setName("partycommandsblacklist", "pcbl")
        description("Manages the party command blacklist")
        runs { showBlacklist() }

        literal("add") {
            argument("player", StringArgumentType.word()) {
                suggests { PartyUtils.members.filterNot(::isBlacklisted) }
                runs {
                    val name = StringArgumentType.getString(it, "player")
                    val added = playerBlacklist.get().add(name.lowercase())
                    if (added) playerBlacklist.save()
                    ChatUtils.modMessage(if (added) "&aAdded &b$name &ato the party command blacklist." else "&e$name is already blacklisted.")
                }
            }
        }

        literal("remove") {
            argument("player", StringArgumentType.word()) {
                suggests { playerBlacklist.get().sorted() }
                runs {
                    val name = StringArgumentType.getString(it, "player")
                    val removed = playerBlacklist.get().remove(name.lowercase())
                    if (removed) playerBlacklist.save()
                    ChatUtils.modMessage(if (removed) "&aRemoved &b$name &afrom the party command blacklist." else "&e$name is not blacklisted.")
                }
            }
        }

        literal("list") {
            runs { showBlacklist() }
        }

        literal("clear") {
            runs {
                playerBlacklist.get().clear()
                playerBlacklist.save()
                ChatUtils.modMessage("&aCleared the party command blacklist.")
            }
        }
    }

    override fun init() {
        register<PacketEvent.Sent> {
            if (! LocationUtils.onHypixel || ! partyAddons.value) return@register
            if (event.packet !is ServerboundChatCommandPacket) return@register
            if (event.packet.command.lowercase().equalsOneOf("pl", "party list", "p list")) {
                awaitingDelimiter = 2
            }
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.onHypixel) return@register

            if (partyCommands.value) partyCommandRegex.find(event.unformattedText)?.let { match ->
                val (name, _, cmdAll) = match.destructured
                val args = cmdAll.split(" ").toMutableList()
                val cmd = args.removeAt(0).lowercase()
                handlePartyCommand(name, cmd, args)
                return@register
            }

            if (partyAddons.value && awaitingDelimiter > 0) {
                handlePartyListParsing(event)
            }
        }

        register<DungeonEvent.RunEndedEvent> {
            if (downtimeList.isEmpty()) return@register
            val names = downtimeList.keys.joinToString(", ")
            ChatUtils.showTitle("&cDowntime!", "Players needing DT: $names")
            USound.playSoundStatic(SoundEvents.NOTE_BLOCK_PLING, 0.25f, 1f)
            ChatUtils.sendPartyMessage("Players needing DT: $names")
            downtimeList.clear()
        }
    }

    private fun handlePartyCommand(sender: String, cmd: String, args: List<String>) {
        if (isBlacklisted(sender)) return

        fun canRun(key: String) = commands.value[key] == true

        when {
            canRun("!fps") && cmd == "fps" -> ChatUtils.sendPartyMessage("FPS: ${mc.fps}")

            canRun("!f") && cmd.startsWith("f") -> {
                val floor = cmd.removePrefix("f").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return
                if (floor in 0 .. 7) runCommand("joininstance CATACOMBS_FLOOR_${DungeonUtils.FLOOR_NAMES[floor]}", true)
            }

            canRun("!m") && cmd.startsWith("m") -> {
                val floor = cmd.removePrefix("m").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return
                if (floor in 1 .. 7) runCommand("joininstance MASTER_CATACOMBS_FLOOR_${DungeonUtils.FLOOR_NAMES[floor]}", true)
            }

            canRun("!pt") && cmd.equalsOneOf("pt", "ptme") -> {
                if (sender != mc.user.name) runCommand("p transfer $sender", true)
            }

            canRun("!coords") && cmd.equalsOneOf("coords", "cords") -> {
                runCommand("pc x: ${player.blockX}, y: ${player.blockY}, z: ${player.blockZ}")
            }

            canRun("!dt") && cmd.equalsOneOf("dt", "downtime") -> {
                downtimeList[sender] = args.joinToString(" ").ifBlank { "No reason" }
            }

            canRun("!w") && cmd.equalsOneOf("warp", "w") -> runCommand("p warp", true)

            canRun("!ai") && cmd.equalsOneOf("ai", "allinvite") -> runCommand("p settings allinvite", true)

            canRun("!ping") && cmd == "ping" -> ChatUtils.sendPartyMessage("Ping: ${ServerUtils.currentPing}ms")

            canRun("!tps") && cmd == "tps" -> ChatUtils.sendPartyMessage("TPS: ${ServerUtils.tps.toFixed(1)}")

            canRun("!kick") && cmd.equalsOneOf("kick", "k") -> {
                if (args.isEmpty()) return
                PartyUtils.members.find { it.contains(args[0], true) }?.let {
                    runCommand("p kick $it", true)
                }
            }

            canRun("!inv") && cmd.equalsOneOf("inv", "kidnap", "invite") -> {
                args.firstOrNull()?.let { runCommand("p invite $it", true) }
            }

            canRun("!gay") && cmd == "gay" -> {
                val target = args.firstOrNull() ?: sender
                val gayPercentage = (Math.random() * 100).roundToInt().coerceIn(0, 100)
                runCommand("pc $target is $gayPercentage% gay.")
            }
        }
    }

    private fun isBlacklisted(name: String) = name.lowercase() in playerBlacklist.get()

    private fun showBlacklist() {
        val names = playerBlacklist.get().sorted()
        ChatUtils.modMessage(if (names.isEmpty()) "&eThe party command blacklist is empty." else "&aParty command blacklist: &f${names.joinToString(", ")}")
    }

    private fun handlePartyListParsing(event: ChatMessageEvent) {
        val unformatted = event.unformattedText
        val formatted = event.formattedText

        when {
            unformatted.isBlank() -> event.isCanceled = true

            partyStartPattern.matches(unformatted) -> {
                party.clear()
                event.isCanceled = true
            }

            unformatted.startsWithOneOf("Party Leader: ", "Party Moderators: ", "Party Members: ") -> {
                val type = when {
                    unformatted.startsWith("Party Leader") -> PartyMemberType.LEADER
                    unformatted.startsWith("Party Moderators") -> PartyMemberType.MODERATOR
                    else -> PartyMemberType.MEMBER
                }
                playerPattern.findAll(formatted.substringAfter(": ")).forEach {
                    val (rank, name, status) = it.destructured
                    party.add(PartyMember(name, type, "§$status", rank))
                }
                event.isCanceled = true
            }

            unformatted.startsWith("You are not currently in a party") -> {
                party.clear()
                awaitingDelimiter = 0
            }

            unformatted.startsWith("-----") -> {
                event.isCanceled = true
                awaitingDelimiter --
                if (awaitingDelimiter == 0 && party.isNotEmpty()) formatPartyList()
            }
        }
    }

    private fun formatPartyList() {
        val main = Component.literal("§9§m§l----------------------------------\n").append("  §aParty Members (${party.size})\n")

        val isLeader = party.any { it.name == mc.user.name && it.type == PartyMemberType.LEADER }

        if (isLeader) main.apply {
            append(createButton("  §9[Warp] ", "/p warp", "§7Warp Party"))
            append(createButton("§e[Invite] ", "/p settings allinvite", "§7Toggle AllInvite"))
            append(createButton("§4[Disband]\n", "/p disband", "§c§lBE CAREFUL"))
        }

        party.sortedBy { it.type }.forEach { m ->
            val color = if (m.status.contains("a") || m.status.contains("§a")) "§a" else "§c"
            val line = Component.literal("\n  $color● §r${m.rank}${m.name}")
            if (m.type == PartyMemberType.LEADER) line.append(" §e(Leader)")

            if (isLeader && m.name != mc.user.name) {
                line.append(createButton(" §c[Kick]", "/p kick ${m.name}", "§cKicks ${m.name}"))
                line.append(createButton(" §a[Transfer]", "/p Transfer ${m.name}", "§aTransfers the party to &f${m.name}"))
            }
            main.append(line)
        }

        main.append("\n§9§m§l----------------------------------")
        ChatUtils.chat(main)
    }

    private fun runCommand(cmd: String, leaderReq: Boolean = false) {
        if (leaderReq && partyLeaderCheck.value && ! isLeader()) return
        ChatUtils.sendCommand(cmd)
    }

    private fun createButton(text: String, command: String, hover: String): MutableComponent {
        return Component.literal(text.addColor()).withStyle {
            it.withClickEvent(ClickEvent.RunCommand(command))
                .withHoverEvent(HoverEvent.ShowText(Component.literal(hover.addColor())))
        }
    }

    private data class PartyMember(val name: String, val type: PartyMemberType, val status: String, val rank: String)
    private enum class PartyMemberType { LEADER, MODERATOR, MEMBER }
}