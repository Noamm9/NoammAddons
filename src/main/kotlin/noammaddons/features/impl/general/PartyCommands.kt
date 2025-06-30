package noammaddons.features.impl.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.DungeonEvent
import noammaddons.features.Feature
import noammaddons.features.impl.hud.TpsDisplay
import noammaddons.NoammAddons.Companion.CHAT_PREFIX
import noammaddons.ui.config.core.impl.SeperatorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
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
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.removeIf
import kotlin.math.roundToInt

object PartyCommands: Feature("Allows Party members to execute leader commands with chat cmds") {
    private val partyLeaderCheck = ToggleSetting("Party Leader Check", false)

    private val pcWarp = ToggleSetting("!w")
    private val pcFloor = ToggleSetting("!f (0-7)")
    private val pcMaster = ToggleSetting("!m (0-7)")
    private val pcInv = ToggleSetting("!inv")
    private val pcDowntime = ToggleSetting("!dt")
    private val pcPing = ToggleSetting("!ping")
    private val pcTps = ToggleSetting("!tps")
    private val pcPT = ToggleSetting("!pt")
    private val pcAllInvite = ToggleSetting("!ai")
    private val pcCoords = ToggleSetting("!coords")
    private val pcGay = ToggleSetting("!gay")

    override fun init() = addSettings(
        partyLeaderCheck,
        SeperatorSetting("Commands"),
        pcWarp, pcFloor, pcMaster,
        pcInv, pcDowntime, pcPing,
        pcTps, pcPT, pcAllInvite,
        pcCoords,
        SeperatorSetting("Funny"),
        pcGay
    )

    private val partyCommandRegex = Regex("^Party > (?:\\[[^]]+] )?([^:]+): ([!?.\\-@#`/])(.+)$")
    val downtimeList = mutableMapOf<String, String>()
    val NUMBERS_TO_TEXT = mapOf(
        0 to "ENTRANCE", 1 to "ONE", 2 to "TWO", 3 to "THREE",
        4 to "FOUR", 5 to "FIVE", 6 to "SIX", 7 to "SEVEN"
    )

    fun runCommand(command: String, needLeader: Boolean = false) {
        if (partyLeaderCheck.value && needLeader && PartyUtils.leader != mc.session.username) return
        sendChatMessage("/$command")
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        val match = partyCommandRegex.find(event.component.noFormatText) ?: return
        val (name, sign, commandString) = match.destructured
        var args = commandString.split(" ")
        val command = args.firstOrNull()?.lowercase() ?: return@onChat
        args = args.drop(1)

        when {
            pcFloor.value && command.startsWith("f") -> {
                val floorNumber = command.remove("f").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return@onChat
                if (floorNumber !in 0 .. 7) return@onChat
                runCommand("joininstance CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
            }

            pcMaster.value && command.startsWith("m") -> {
                val floorNumber = command.remove("m").toIntOrNull() ?: args.getOrNull(0)?.toIntOrNull() ?: return@onChat
                if (floorNumber !in 1 .. 7) return@onChat
                runCommand("joininstance MASTER_CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
            }

            pcPT.value && command.equalsOneOf("pt", "ptme") -> {
                if (name == mc.session.username) return@onChat
                runCommand("p transfer ${args.firstOrNull() ?: name}", true)
            }

            pcWarp.value && command.equalsOneOf("w", "warp") -> {
                runCommand("p warp", true)
            }

            pcAllInvite.value && command.equalsOneOf("ai", "allinv", "allinvite") -> {
                runCommand("p settings allinvite", true)
            }

            pcCoords.value && command.equalsOneOf("cords", "coords") -> {
                val (x, y, z) = mc.thePlayer.position.destructured()
                runCommand("pc x: $x, y: $y, z: $z")
            }

            pcTps.value && command == "tps" -> {
                runCommand("pc ${CHAT_PREFIX.removeFormatting()} ${TpsDisplay.getTps()}")
            }

            pcDowntime.value && command.equalsOneOf("dt", "downtime") -> {
                downtimeList[name] = if (args.isEmpty()) "No Reason Provided" else args.joinToString(" ")
            }

            pcGay.value && command == "gay" -> {
                val target = args.firstOrNull() ?: name
                val gayPercentage = (Math.random() * 100).roundToInt().coerceIn(0, 100)
                runCommand("pc $target is $gayPercentage% gay.")
            }

            pcPing.value && command == "ping" -> getPing { ping ->
                runCommand("pc ${CHAT_PREFIX.removeFormatting()} Ping: ${ping}ms")
            }

            pcInv.value && command.equalsOneOf("invite", "inv", "kidnap") -> {
                if (args.isEmpty()) return@onChat
                runCommand("p invite ${args.joinToString(" ")}", true)
            }
        }
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
}

