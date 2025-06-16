package noammaddons.commands.commands

import kotlinx.coroutines.*
import net.minecraft.command.ICommandSender
import net.minecraft.util.ChatComponentText
import noammaddons.commands.Command
import noammaddons.features.impl.misc.ClientTimer.cmd
import noammaddons.features.impl.misc.ClientTimer.cmdAlways
import noammaddons.features.impl.misc.ClientTimer.mode
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage


object TimerCommand: Command("timer") {
    private var timerJob: Job? = null
    private var startTime = System.currentTimeMillis()
    private var time = 0L

    private val actions = arrayOf(
        { mc.netHandler?.networkManager?.closeChannel(ChatComponentText("$FULL_PREFIX&r: Timer Ended")) },
        { mc.shutdown() },
        {
            val slash = if (cmd.contains("/")) "" else "/"
            val cmd = cmd.removeFormatting()
            if (cmd.isNotBlank()) sendChatMessage(slash + cmd)
        },
    )

    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            if (timerJob?.isActive == true) {
                timerJob?.cancel()
                timerJob = null
                modMessage("&aTimer &ccancelled &awith &e${formatTime((time - (System.currentTimeMillis() - startTime)) / 1000)}&a left.")
                startTime = 0
                time = 0
            }
            else modMessage("&cInvalid Usage. &bUsage: /timer <time>\n  Example: /na timer 1h 2m 10s")
            return
        }

        val seconds = parseTimeArg(args.joinToString(" ")).takeIf { it > 0 } ?: return modMessage("&cInvalid time. Please provide a positive duration.")
        val action = when (mode) {
            0 -> "Disconnect"
            1 -> "Close Game"
            2 -> "Run Command ($cmd)"
            else -> ""
        }

        modMessage("&aTimer set for &e${formatTime(seconds.toLong())}&a. &bAction: $action")
        startTime = System.currentTimeMillis()
        time = seconds * 1000L

        timerJob?.cancel()
        timerJob = scope.launch {
            delay(seconds * 1000L)
            if (cmdAlways && mode != 2 && cmd.removeFormatting().isNotBlank()) {
                sendChatMessage(cmd)
            }

            actions.getOrNull(mode)?.invoke()
        }
    }

    private fun parseTimeArg(input: String): Int {
        val units = listOf(
            Regex("""(\d+)\s*d(ays?)?""", RegexOption.IGNORE_CASE) to 86400,
            Regex("""(\d+)\s*h(ours?)?""", RegexOption.IGNORE_CASE) to 3600,
            Regex("""(\d+)\s*m(in(utes?)?)?""", RegexOption.IGNORE_CASE) to 60,
            Regex("""(\d+)\s*s(ec(onds?)?)?""", RegexOption.IGNORE_CASE) to 1,
        )

        var totalSeconds = 0
        var matched = false

        for ((regex, multiplier) in units) {
            regex.findAll(input).forEach { match ->
                val value = match.groupValues[1].toIntOrNull() ?: 0
                totalSeconds += value * multiplier
                matched = true
            }
        }

        if (! matched) totalSeconds = input.trim().toIntOrNull() ?: 0
        return totalSeconds
    }

    private fun formatTime(seconds: Long): String {
        if (seconds <= 0) return "0s"

        val parts = mutableListOf<String>()
        var remaining = seconds

        val units = listOf(
            86400L to "d",
            3600L to "h",
            60L to "m",
            1L to "s"
        )

        for ((unitSeconds, suffix) in units) {
            val value = remaining / unitSeconds
            if (value > 0) {
                parts += "${value}${suffix}"
                remaining %= unitSeconds
            }
        }

        return parts.joinToString(" ")
    }

}