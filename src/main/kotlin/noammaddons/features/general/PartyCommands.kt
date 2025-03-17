package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.features.hud.TpsDisplay
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.getPing
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.Utils.equalsOneOf
import kotlin.math.roundToInt

object PartyCommands: Feature() {
    private val partyCommandRegex = Regex("^Party > (?:\\[[^]]+] )?([^:]+): ([!?.\\-@#`/])(.+)$")
    val downtimeList = mutableListOf<Pair<String, String>>()
    val NUMBERS_TO_TEXT = mapOf(
        0 to "ENTRANCE",
        1 to "ONE",
        2 to "TWO",
        3 to "THREE",
        4 to "FOUR",
        5 to "FIVE",
        6 to "SIX",
        7 to "SEVEN"
    )

    fun runCommand(command: String, needLeader: Boolean = false) {
        if (needLeader && ! PartyUtils.isPartyLeader()) return
        sendChatMessage("/$command")
    }


    init {
        DungeonUtils.dungeonEnded.onSetValue {
            if (! it) return@onSetValue
            if (downtimeList.isEmpty()) return@onSetValue
            val dtMessage = downtimeList.joinToString(", ") { (username, reason) -> username/*: $reason"*/ } // !dt ip abuse ban
            val grammer = if (downtimeList.size == 1) "Player" else "Players"
            SoundUtils.notificationSound.start()

            showTitle("&4&lDOWNTIME!!!", "${downtimeList.size} $grammer Need DT!", 5)
            sendPartyMessage("$grammer Need DT: $dtMessage")

            downtimeList.clear()
        }
    }

    @SubscribeEvent
    fun trigger(event: Chat) {
        if (! config.PartyCommands) return
        val msg = event.component.noFormatText

        partyCommandRegex.find(msg)?.let {
            val (name, sign, commandString) = it.destructured

            var args = commandString.split(" ")
            val command = args.firstOrNull()?.lowercase() ?: return@let
            args = args.drop(1)

            when {
                config.pcFloor && command.startsWith("f") -> {
                    val floorNumber = command.replace("f", "").toIntOrNull() ?: args[0].toIntOrNull() ?: return@let
                    if (floorNumber !in 0 .. 7) return@let
                    runCommand("joininstance CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
                }

                config.pcMasterFloor && command.startsWith("m") -> {
                    val floorNumber = command.replace("m", "").toIntOrNull() ?: args[0].toIntOrNull() ?: return@let
                    if (floorNumber !in 1 .. 7) return@let
                    runCommand("joininstance MASTER_CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
                }

                config.pcPtme && command.equalsOneOf("pt", "ptme") -> {
                    if (name == mc.session.username) return@let
                    runCommand("p transfer $name ${args.firstOrNull() ?: name}", true)
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
                    downtimeList.add(name to if (args.isEmpty()) "No Reason Provided" else args.joinToString(" "))
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
                    if (args.isEmpty()) return@let
                    runCommand("p invite ${args.joinToString(" ")}", true)
                }
            }
        }
    }
}

