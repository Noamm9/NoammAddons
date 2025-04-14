package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.features.hud.TpsDisplay
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.utils.ChatUtils.getPing
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.PartyUtils
import noammaddons.utils.SoundUtils
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.removeIf
import kotlin.math.roundToInt

object PartyCommands: Feature() {
    private val partyCommandRegex = Regex("^Party > (?:\\[[^]]+] )?([^:]+): ([!?.\\-@#`/])(.+)$")
    val downtimeList = mutableMapOf<String, String>()
    val NUMBERS_TO_TEXT = mapOf(
        0 to "ENTRANCE", 1 to "ONE", 2 to "TWO", 3 to "THREE",
        4 to "FOUR", 5 to "FIVE", 6 to "SIX", 7 to "SEVEN"
    )

    fun runCommand(command: String, needLeader: Boolean = false) {
        if (needLeader && PartyUtils.leader != mc.session.username) return
        sendChatMessage("/$command")
    }

    @SubscribeEvent
    fun onRunEnd(event: DungeonEvent.RunEndedEvent) {
        downtimeList.removeIf { it.key !in PartyUtils.members.keys }
        if (downtimeList.isEmpty()) return

        val partyDtMsg = downtimeList.keys.joinToString(", ")
        val dtMessage = downtimeList.entries.joinToString(", ") { (username, reason) -> "$username: $reason" }
        val grammer = if (downtimeList.size == 1) "Player" else "Players"
        SoundUtils.notificationSound()

        showTitle("&4&lDOWNTIME!!!", "${downtimeList.size} $grammer Need DT!", 10)
        sendPartyMessage("$grammer Need DT: $partyDtMsg")
        modMessage("$grammer Need DT: $dtMessage")
        downtimeList.clear()
    }


    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.PartyCommands) return
        val (name, sign, commandString) = partyCommandRegex.find(event.component.noFormatText)?.destructured ?: return

        var args = commandString.split(" ")
        val command = args.firstOrNull()?.lowercase() ?: return
        args = args.drop(1)

        when {
            config.pcFloor && command.startsWith("f") -> {
                val floorNumber = command.replace("f", "").toIntOrNull() ?: args[0].toIntOrNull() ?: return
                if (floorNumber !in 0 .. 7) return
                runCommand("joininstance CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
            }

            config.pcMasterFloor && command.startsWith("m") -> {
                val floorNumber = command.replace("m", "").toIntOrNull() ?: args[0].toIntOrNull() ?: return
                if (floorNumber !in 1 .. 7) return
                runCommand("joininstance MASTER_CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
            }

            config.pcPtme && command.equalsOneOf("pt", "ptme") -> {
                if (name == mc.session.username) return
                runCommand("p transfer ${args.firstOrNull() ?: name}", true)
            }

            config.pcWarp && command.equalsOneOf("w", "warp") -> {
                runCommand("p warp", true)
            }

            config.pcAllinv && command.equalsOneOf("ai", "allinv", "allinvite") -> {
                runCommand("p settings allinvite", true)
            }

            config.pcCoords && command.equalsOneOf("cords", "coords") -> {
                val (x, y, z) = mc.thePlayer.position.destructured()
                runCommand("pc x: $x, y: $y, z: $z")
            }

            config.pcTPS && command == "tps" -> {
                runCommand("pc ${CHAT_PREFIX.removeFormatting()} ${TpsDisplay.getTps()}")
            }

            config.pcDt && command.equalsOneOf("dt", "downtime") -> {
                downtimeList[name] = if (args.isEmpty()) "No Reason Provided" else args.joinToString(" ")
            }

            config.pcGay && command == "gay" -> {
                val target = args.firstOrNull() ?: name
                val gayPercentage = (Math.random() * 100).roundToInt().coerceIn(0, 100)
                runCommand("pc $target is $gayPercentage% gay.")
            }

            config.pcPing && command == "ping" -> getPing { ping ->
                runCommand("pc ${CHAT_PREFIX.removeFormatting()} Ping: ${ping}ms")
            }

            config.pcInv && command.equalsOneOf("invite", "inv", "kidnap") -> {
                if (args.isEmpty()) return
                runCommand("p invite ${args.joinToString(" ")}", true)
            }
        }
    }
}

