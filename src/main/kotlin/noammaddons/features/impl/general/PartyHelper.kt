package noammaddons.features.impl.general

import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.CHAT_PREFIX
import noammaddons.events.*
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.removeIf
import kotlin.math.roundToInt


object PartyHelper: Feature("Party Commands, /pl reformatting") {
    private val partyCommands = ToggleSetting("Party Commands ")
    private val partyLeaderCheck = ToggleSetting("Party Commands Leader Check", false).addDependency(partyCommands)
    private val commands = MultiCheckboxSetting(
        "Commands", mapOf(
            "!w" to false, "!f (0-7)" to false,
            "!m (0-7)" to false, "!inv" to false,
            "!kick" to false, "!dt" to false,
            "!ping" to false, "!tps" to false,
            "!pt" to false, "!ai" to false,
            "!coords" to false, "!gay" to false
        )
    ).addDependency(partyCommands) as MultiCheckboxSetting

    private val partyAddons = ToggleSetting("Reformat Party list Message")

    override fun init() = addSettings(
        SeperatorSetting("Party Commands"),
        partyCommands, partyLeaderCheck, commands,
        SeperatorSetting("Party Addons"),
        partyAddons
    )


    private val partyStartPattern = Regex("^§6Party Members \\((\\d+)\\)§r$")
    private val playerPattern = Regex("(?<rank>§r§.(?:\\[.+?] )?)(?<name>\\w+) ?§r(?<status>§a|§c) ?● ?")
    private val party = mutableListOf<PartyMember>()
    private val partyListCommands = setOf("/pl", "/party list", "/p list", "/party l")
    private var awaitingDelimiter = 0 // 0 = not awaiting, 1 = awaiting 2nd delimiter, 2 = awaiting 1st delimiter

    private val partyCommandRegex = Regex("^Party > (?:\\[[^]]+] )?([^:]+): ([!?.\\-@#`/])(.+)$")
    val downtimeList = mutableMapOf<String, String>()
    val NUMBERS_TO_TEXT = mapOf(
        0 to "ENTRANCE", 1 to "ONE", 2 to "TWO", 3 to "THREE",
        4 to "FOUR", 5 to "FIVE", 6 to "SIX", 7 to "SEVEN"
    )

    @SubscribeEvent
    fun onCommandSent(event: MessageSentEvent) {
        if (! onHypixel || ! partyAddons.value) return
        if (event.message.lowercase() in partyListCommands) {
            awaitingDelimiter = 2
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! onHypixel) return

        if (partyCommands.value) partyCommandRegex.find(event.component.noFormatText)?.let { match ->
            val (name, sign, commandString) = match.destructured
            var args = commandString.split(" ")
            val command = args.firstOrNull()?.lowercase() ?: return
            args = args.drop(1)
            handlePartyCommand(name, command, args)
            return
        }

        if (partyAddons.value && awaitingDelimiter > 0) handlePartyListParsing(event.component.formattedText, event)
    }

    @SubscribeEvent
    fun onRunEnd(event: DungeonEvent.RunEndedEvent) {
        downtimeList.removeIf { it.key !in PartyUtils.members.keys }
        if (downtimeList.isEmpty()) return

        val partyDtMsg = downtimeList.keys.joinToString(", ")
        val grammar = if (downtimeList.size == 1) "Player" else "Players"

        SoundUtils.notificationSound()
        showTitle("&4&lDOWNTIME!!!", "${downtimeList.size} $grammar Need DT!", 10)
        sendPartyMessage("$grammar Need DT: $partyDtMsg")
        downtimeList.clear()
    }

    private fun handlePartyCommand(name: String, command: String, args: List<String>) {
        when {
            commands.get("!f (0-7)") && command.startsWith("f") -> {
                val floorNumber = command.removePrefix("f").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return
                if (floorNumber in 0 .. 7) {
                    runCommand("joininstance CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
                }
            }

            commands.get("!m (0-7)") && command.startsWith("m") -> {
                val floorNumber = command.removePrefix("m").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return
                if (floorNumber in 1 .. 7) {
                    runCommand("joininstance MASTER_CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
                }
            }

            commands.get("!pt") && command.equalsOneOf("pt", "ptme") -> {
                if (name == mc.session.username) return
                runCommand("p transfer ${args.firstOrNull() ?: name}", true)
            }

            commands.get("!w") && command.equalsOneOf("w", "warp") -> runCommand("p warp", true)
            commands.get("!ai") && command.equalsOneOf("ai", "allinv", "allinvite") -> runCommand("p settings allinvite", true)

            commands.get("!coords") && command.equalsOneOf("cords", "coords") -> {
                val (x, y, z) = mc.thePlayer.position.destructured()
                runCommand("pc x: $x, y: $y, z: $z")
            }

            commands.get("!tps") && command == "tps" -> runCommand("pc ${CHAT_PREFIX.removeFormatting()} TPS: ${ServerUtils.averageTps}")
            commands.get("!ping") && command == "ping" -> runCommand("pc ${CHAT_PREFIX.removeFormatting()} Ping: ${ServerUtils.latestPing}ms")

            commands.get("!dt") && command.equalsOneOf("dt", "downtime") -> {
                downtimeList[name] = if (args.isEmpty()) "No Reason Provided" else args.joinToString(" ")
            }

            commands.get("!gay") && command == "gay" -> {
                val target = args.firstOrNull() ?: name
                val gayPercentage = (Math.random() * 100).roundToInt().coerceIn(0, 100)
                runCommand("pc $target is $gayPercentage% gay.")
            }

            commands.get("!inv") && command.equalsOneOf("invite", "inv", "kidnap") -> {
                if (args.isNotEmpty()) runCommand("p invite ${args.joinToString(" ")}", true)
            }

            commands.get("!kick") && command.equalsOneOf("kick", "k") -> {
                if (args.isEmpty()) return
                val memberToKick = PartyUtils.members.keys.find { it.lowercase().contains(args.first().lowercase()) } ?: return
                runCommand("p kick $memberToKick", true)
            }
        }
    }

    private fun handlePartyListParsing(formattedText: String, event: Chat) {
        when {
            formattedText == "§f§r" -> event.isCanceled = true
            partyStartPattern.matches(formattedText) -> {
                party.clear()
                event.isCanceled = true
            }

            formattedText.startsWith("§eParty ") -> {
                val playerType = when {
                    formattedText.startsWith("§eParty Leader: ") -> PartyMemberType.LEADER
                    formattedText.startsWith("§eParty Moderators: ") -> PartyMemberType.MODERATOR
                    formattedText.startsWith("§eParty Members: ") -> PartyMemberType.MEMBER
                    else -> return
                }
                playerPattern.findAll(formattedText.substringAfter(": ")).forEach {
                    it.destructured.let { (rank, name, status) ->
                        party.add(PartyMember(name, playerType, status, rank))
                    }
                }
                event.isCanceled = true
            }

            formattedText.startsWith("§cYou are not currently in a party.") -> party.clear()
            event.component.noFormatText.startsWith("-----") -> {
                event.isCanceled = true
                awaitingDelimiter --
                if (awaitingDelimiter == 0 && party.isNotEmpty()) {
                    formatPartyList()
                }
            }
        }
    }

    private fun formatPartyList() {
        val component = ChatComponentText("§9§m§l―――――――――――――――――――――――§r\n")
        component.appendText("  §aParty members (${party.size})\n")
        val self = party.find { it.name == mc.session.username } ?: return

        if (self.type == PartyMemberType.LEADER) {
            component.appendSibling(createButton("  §9[Warp] ", "/p warp", "§9Click to warp the party."))
                .appendSibling(createButton("§e[All Invite] ", "/p settings allinvite", "§eClick to toggle all invite."))
                .appendSibling(createButton("§6[Mute]\n", "/p mute", "§6Click to toggle mute."))
                .appendSibling(createButton("  §c[Kick Offline] ", "/p kickoffline", "§cClick to kick offline members."))
                .appendSibling(createButton("§4[Disband]\n", "/p disband", "§4Click to disband the party."))
        }

        party.sortedBy { it.type }.forEach { member ->
            val prefix = when (member.type) {
                PartyMemberType.LEADER -> "\n  ${member.status}➡§r "
                PartyMemberType.MODERATOR -> "\n  ${member.status}➡§r "
                PartyMemberType.MEMBER -> "\n  ${member.status}➡§r "
            }

            component.appendText(prefix + member.rank + member.name + if (member.type == PartyMemberType.LEADER) " §e(Leader)§r" else "")

            if (self.type == PartyMemberType.LEADER && member.name != self.name) {
                when (member.type) {
                    PartyMemberType.MODERATOR -> {
                        component.appendSibling(createButton(" §a[⋀] ", "/p promote ${member.name}", "§aPromote ${member.name}"))
                            .appendSibling(createButton("§c[⋁] ", "/p demote ${member.name}", "§cDemote ${member.name}"))
                    }

                    PartyMemberType.MEMBER -> {
                        component.appendSibling(createButton(" §9[⋀] ", "/p transfer ${member.name}", "§9Transfer party to ${member.name}"))
                            .appendSibling(createButton("§a[⋀] ", "/p promote ${member.name}", "§aPromote ${member.name}"))
                    }

                    else -> {} // Leader can't act on themselves
                }
                component.appendSibling(createButton("§4[✖] ", "/p kick ${member.name}", "§4Kick ${member.name}"))
                    .appendSibling(createButton("§7[B]", "/block add ${member.name}", "§7Block ${member.name}"))
            }
        }

        component.appendSibling(ChatComponentText("\n§9§m§l―――――――――――――――――――――――§r"))
        mc.addScheduledTask { mc.thePlayer.addChatMessage(component) }
    }


    private fun runCommand(command: String, needLeader: Boolean = false) {
        if (partyLeaderCheck.value && needLeader && PartyUtils.leader != mc.session.username) return
        sendChatMessage("/$command")
    }

    private fun createButton(text: String, command: String, hoverText: String): ChatComponentText {
        return ChatComponentText(text.addColor()).apply {
            chatStyle = ChatStyle().apply {
                chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText.addColor()))
                chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
            }
        }
    }


    private data class PartyMember(val name: String, val type: PartyMemberType, val status: String, val rank: String)
    private enum class PartyMemberType: Comparable<PartyMemberType> { LEADER, MODERATOR, MEMBER }
}