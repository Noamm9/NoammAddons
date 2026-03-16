package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.MultiCheckboxSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.PartyUtils
import com.github.noamm9.utils.PartyUtils.isLeader
import com.github.noamm9.utils.ServerUtils
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.sounds.SoundEvents
import kotlin.math.roundToInt

object PartyHelper: Feature("Party commands and reformatting.") {
    private val partyCommands by ToggleSetting("Party Commands", true)
        .section("Party Commands")

    private val partyLeaderCheck by ToggleSetting("Leader Only", false)
        .showIf { partyCommands.value }

    private val commands by MultiCheckboxSetting("Enabled Commands", mutableMapOf(
        "!w" to true, "!f" to true, "!m" to true, "!inv" to true,
        "!kick" to true, "!dt" to true, "!ping" to true, "!tps" to true,
        "!pt" to true, "!ai" to true, "!coords" to true, "!gay" to true
    )).showIf { partyCommands.value }

    private val partyAddons by ToggleSetting("Reformat Party List", true)
        .section("Party Addons")

    private val party = mutableListOf<PartyMember>()
    val downtimeList = mutableMapOf<String, String>()
    private var awaitingDelimiter = 0

    private val partyStartPattern = Regex("^Party Members \\((\\d+)\\)$")
    private val playerPattern = Regex("(?<rank>(?:\\[.+?] )?)(?<name>\\w+) ?(?<status>.) ?● ?")
    private val partyCommandRegex = Regex("^Party > (?:\\[[^]]+] )?([^:]+): ([!?.\\-@#`/])(.+)$")

    override fun init() {
        register<PacketEvent.Sent> {
            if (! LocationUtils.onHypixel || ! partyAddons.value) return@register
            if (event.packet !is ServerboundChatPacket) return@register
            val msg = event.packet.message.lowercase()
            if (msg == "/pl" || msg == "/party list" || msg == "/p list") {
                awaitingDelimiter = 2
            }
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.onHypixel) return@register
            val unformatted = event.unformattedText

            if (partyCommands.value) {
                partyCommandRegex.find(unformatted)?.let { match ->
                    val (name, sign, cmdAll) = match.destructured
                    val args = cmdAll.split(" ").toMutableList()
                    val cmd = args.removeAt(0).lowercase()
                    handlePartyCommand(name, cmd, args)
                    return@register
                }
            }

            if (partyAddons.value && awaitingDelimiter > 0) {
                handlePartyListParsing(unformatted, event)
            }
        }

        register<DungeonEvent.RunEndedEvent> {
            if (downtimeList.isEmpty()) return@register
            val names = downtimeList.keys.joinToString(", ")
            ChatUtils.showTitle("&cDowntime!", "Players needing DT: $names")
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1f))
            ChatUtils.sendPartyMessage("Players needing DT: $names")
            downtimeList.clear()
        }
    }

    private fun handlePartyCommand(sender: String, cmd: String, args: List<String>) {
        fun canRun(key: String) = commands.value[key] == true

        when {
            canRun("!f") && cmd.startsWith("f") -> {
                val floor = cmd.removePrefix("f").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return
                if (floor in 0 .. 7) runCommand("joininstance CATACOMBS_FLOOR_${DungeonUtils.FLOOR_NAMES[floor]}", true)
            }

            canRun("!m") && cmd.startsWith("m") -> {
                val floor = cmd.removePrefix("m").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return
                if (floor in 1 .. 7) runCommand("joininstance MASTER_CATACOMBS_FLOOR_${DungeonUtils.FLOOR_NAMES[floor]}", true)
            }

            canRun("!pt") && (cmd == "pt" || cmd == "ptme") -> {
                if (sender != mc.user.name) runCommand("p transfer $sender", true)
            }

            canRun("!coords") && (cmd == "coords" || cmd == "cords") -> {
                val p = mc.player ?: return
                runCommand("pc x: ${p.blockX}, y: ${p.blockY}, z: ${p.blockZ}")
            }

            canRun("!dt") && (cmd == "dt" || cmd == "downtime") -> {
                downtimeList[sender] = args.joinToString(" ").ifBlank { "No reason" }
            }

            canRun("!w") && (cmd == "warp" || cmd == "w") -> runCommand("p warp", true)

            canRun("!ai") && (cmd == "ai" || cmd == "allinvite") -> runCommand("p settings allinvite", true)

            canRun("!ping") && cmd == "ping" -> ChatUtils.sendPartyMessage("Ping: ${ServerUtils.currentPing}ms")

            canRun("!tps") && cmd == "tps" -> ChatUtils.sendPartyMessage("TPS: ${ServerUtils.tps.toFixed(1)}")

            canRun("!kick") && (cmd == "kick" || cmd == "k") -> {
                if (args.isEmpty()) return
                PartyUtils.members.find { it.contains(args[0], true) }?.let {
                    runCommand("p kick $it", true)
                    ChatUtils.modMessage("$it")
                }
            }

            canRun("!inv") && (cmd == "inv" || cmd == "kidnap" || cmd == "invite") -> runCommand("p invite $args", true)

            canRun("!gay") && cmd == "gay" -> {
                val target = args.firstOrNull() ?: sender
                val gayPercentage = (Math.random() * 100).roundToInt().coerceIn(0, 100)
                runCommand("pc $target is $gayPercentage% gay.")
            }
        }
    }

    private fun handlePartyListParsing(text: String, event: ChatMessageEvent) {
        when {
            partyStartPattern.matches(text) -> {
                party.clear()
                event.isCanceled = true
            }

            text.startsWith("Party Leader: ") || text.startsWith("Party Moderators: ") || text.startsWith("Party Members: ") -> {
                val type = when {
                    text.startsWith("Party Leader") -> PartyMemberType.LEADER
                    text.startsWith("Party Moderators") -> PartyMemberType.MODERATOR
                    else -> PartyMemberType.MEMBER
                }
                playerPattern.findAll(text.substringAfter(": ")).forEach {
                    val (rank, name, status) = it.destructured
                    party.add(PartyMember(name, type, status, rank))
                }
                event.isCanceled = true
            }

            text.startsWith("-----------------") -> {
                event.isCanceled = true
                awaitingDelimiter --
                if (awaitingDelimiter == 0 && party.isNotEmpty()) formatPartyList()
            }
        }
    }

    private fun formatPartyList() {
        val main = Component.literal("§9§m§l----------------------------------\n").append("  §aParty Members (${party.size})\n")

        val isLeader = party.any { it.name == mc.user.name && it.type == PartyMemberType.LEADER }

        if (isLeader) {
            main.append(createButton("  §9[Warp] ", "/p warp", "§7Warp Party"))
                .append(createButton("§e[Invite] ", "/p settings allinvite", "§7Toggle AllInvite"))
                .append(createButton("§4[Disband]\n", "/p disband", "§c§lBE CAREFUL"))
        }

        party.sortedBy { it.type }.forEach { m ->
            val color = if (m.status.contains("a") || m.status.contains("§a")) "§a" else "§c"
            val line = Component.literal("\n  $color● §r${m.rank}${m.name}")
            if (m.type == PartyMemberType.LEADER) line.append(" §e(Leader)")

            if (isLeader && m.name != mc.user.name) {
                line.append(createButton(" §c[Kick]", "/p kick ${m.name}", "§cKick ${m.name}"))
            }
            main.append(line)
        }

        main.append("\n§9§m§l----------------------------------")
        mc.execute { ChatUtils.chat(main) }
    }

    private fun runCommand(cmd: String, leaderReq: Boolean = false) {
        if (leaderReq && partyLeaderCheck.value && ! isLeader()) return
        ChatUtils.sendCommand(cmd)
    }

    private fun createButton(text: String, command: String, hover: String): MutableComponent {
        return Component.literal(text).withStyle {
            it.withClickEvent(ClickEvent.RunCommand(command))
                .withHoverEvent(HoverEvent.ShowText(Component.literal(hover)))
        }
    }

    private data class PartyMember(val name: String, val type: PartyMemberType, val status: String, val rank: String)
    private enum class PartyMemberType { LEADER, MODERATOR, MEMBER }
}