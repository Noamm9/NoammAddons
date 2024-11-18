package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.features.hud.TpsDisplay.getTps
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.utils.ChatUtils.getPing
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.PartyUtils
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout

object PartyCommands: Feature() {
    private val partyCommandRegex = Regex("^Party > (?:\\[[^]]+] )?([^:]+): ([!?.\\-@#`/])(.+)$")
    val commands = mutableMapOf<String, (String, List<String>) -> Unit>()
    const val DELAY = 1000L
    val HELP_MSG = mutableListOf<String>()
    val NUMBERS_TO_TEXT = mapOf(
        1 to "ONE",
        2 to "TWO",
        3 to "THREE",
        4 to "FOUR",
        5 to "FIVE",
        6 to "SIX",
        7 to "SEVEN"
    )

    var lastTimeUsed = System.currentTimeMillis()
    val downtimeList = mutableListOf<Pair<String, String>>()
    var sentDelay = false

    fun newPartyCommand(
        commandName: String,
        settingsKey: () -> Boolean,
        fn: (String, List<String>) -> Unit,
        alias: List<String> = emptyList()
    ) {
        if (! settingsKey()) return

        commands[commandName] = fn
        alias.forEach { aliasName -> commands[aliasName] = fn }
    }

    fun runCommand(command: String, needLeader: Boolean = false) {
        if (needLeader && ! PartyUtils.isPartyLeader()) return

        val timeSinceLastUse = System.currentTimeMillis() - lastTimeUsed
        if (timeSinceLastUse < DELAY) {
            if (! sentDelay) {
                sentDelay = true
                sendPartyMessage("Please wait ${DELAY - timeSinceLastUse}ms")
            }
            return
        }

        setTimeout(300) {
            sendChatMessage("/$command")
            lastTimeUsed = System.currentTimeMillis()
            sentDelay = false
        }
    }

    fun getHelpMessage(): String {
        HELP_MSG.clear()
        HELP_MSG.add("Commands:")
        if (config.pcAllinv) HELP_MSG.add("ai (allinv, allinvite)")
        if (config.pcInv) HELP_MSG.add("inv (invite)")
        if (config.pcDt) HELP_MSG.add("dt (downtime)")
        if (config.pcFloor) HELP_MSG.add("f(0-7)")
        if (config.pcMasterFloor) HELP_MSG.add("m(1-7)")
        if (config.pcPtme) HELP_MSG.add("pt (ptme)")
        if (config.pcWarp) HELP_MSG.add("w (warp)")
        if (config.pcCoords) HELP_MSG.add("coords (cords)")
        if (config.pcTPS) HELP_MSG.add("tps")
        if (config.pcPing) HELP_MSG.add("ping")
        if (config.pcGay) HELP_MSG.add("gay")

        return HELP_MSG.joinToString(", ").replace(":, ", ": ")
    }

    @SubscribeEvent
    fun trigger(event: Chat) {
        if (! config.PartyCommands) return
        val msg = event.component.unformattedText.removeFormatting()

        partyCommandRegex.find(msg)?.let {
            val (name, sign, command) = it.destructured

            val args = command.split(" ")
            val commandName = args.firstOrNull()?.lowercase() ?: return@let
            commands[commandName]?.invoke(name, args.drop(1))

            if (commandName.startsWith("floor") || commandName == "f") {
                if (! config.pcFloor) return@let

                val floorNumber = commandName.replace("floor", "").replace("f", "").toIntOrNull() ?: run {
                    if (args.isEmpty()) return@let
                    return@run args.firstOrNull()?.toIntOrNull() ?: return@let
                }

                if (floorNumber !in 0 .. 7) return@let

                runCommand("joininstance CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[floorNumber]}", true)
            }

            if (commandName.startsWith("master") || commandName.startsWith("m")) {
                if (! config.pcMasterFloor) return@let

                commandName.replace("master", "").replace("m", "").toIntOrNull()?.run {
                    if (args.isEmpty()) return@let
                    if (this !in 1 .. 7) return@let

                    runCommand("joininstance MASTER_CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[this]}", true)
                }
            }
        }

        if (msg == "                             > EXTRA STATS <") {
            if (downtimeList.isEmpty()) return
            val dtMessage = downtimeList.joinToString(", ") { (username, reason) -> "$username: $reason" }
            val gram = if (downtimeList.size == 1) "player" else "players"
            SoundUtils.notificationSound.start()

            showTitle("&4&lDOWNTIME!!!", "${downtimeList.size} $gram need DT!", 5)
            sendPartyMessage("$gram Need DT: $dtMessage")

            downtimeList.clear()
        }

    }


    init {
        newPartyCommand("help", { config.PartyCommands }, { _, _ ->
            runCommand("pc ${getHelpMessage()}")
        })

        newPartyCommand("ptme", { config.pcPtme }, { name, _ ->
            if (name == mc.session.username) return@newPartyCommand

            runCommand("p transfer $name", true)
        }, listOf("pt"))

        newPartyCommand("warp", { config.pcWarp }, { _, _ ->
            runCommand("p warp", true)
        }, listOf("w"))

        newPartyCommand("allinv", { config.pcAllinv }, { _, _ ->
            runCommand("p settings allinvite", true)
        }, listOf("ai, allinvite"))

        newPartyCommand("coords", { config.pcCoords }, { _, _ ->
            runCommand(
                "pc x: ${Player !!.posX.toInt()}, " +
                        "y: ${Player !!.posY.toInt()}, " +
                        "z: ${Player !!.posZ.toInt()}"
            )
        }, listOf("cords"))

        newPartyCommand("tps", { config.pcTPS }, { _, _ ->
            runCommand("pc ${CHAT_PREFIX.removeFormatting()} ${getTps()}")
        })

        newPartyCommand("downtime", { config.pcDt }, { name, words ->
            if (downtimeList.map { it.first }.contains(name)) return@newPartyCommand

            downtimeList.add(name to if (words.isEmpty()) "No Reason Provided" else words.joinToString(" "))
        }, listOf("dt"))

        newPartyCommand("gay", { config.pcGay }, { name, args ->
            val target = args.firstOrNull() ?: name
            val gayPercentage = (Math.random() * 100).toInt()
            runCommand("party chat $target is $gayPercentage% gay.")
        })

        newPartyCommand("ping", { config.pcPing }, { _, _ ->
            getPing {
                runCommand("pc ${CHAT_PREFIX.removeFormatting()} Ping: ${it}ms")
            }
        })

        newPartyCommand("invite", { config.pcInv }, { _, args ->
            if (args.isEmpty()) return@newPartyCommand

            runCommand("p invite ${args.joinToString(" ")}", true)
        }, listOf("inv"))

        newPartyCommand("master", { config.pcMasterFloor }, { _, args ->
            if (args.isEmpty()) return@newPartyCommand
            args.firstOrNull()?.toIntOrNull()?.run {
                if (this !in 1 .. 7) return@newPartyCommand
                runCommand("joininstance MASTER_CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[this]}", true)
            }
        }, listOf("m"))

        newPartyCommand("floor", { config.pcFloor }, { _, args ->
            if (args.isEmpty()) return@newPartyCommand
            args.firstOrNull()?.toIntOrNull()?.run {
                if (this !in 0 .. 7) return@newPartyCommand
                runCommand("joininstance CATACOMBS_FLOOR_${NUMBERS_TO_TEXT[this]}", true)
            }
        }, listOf("f"))
    }
}

