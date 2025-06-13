/**
 * Noamm Addons - Timer Command
 * This file has been automagically been JSdoc'ed by https://axle.coffee using OpenAI's GPT-4.1
 * This file is part of the Noamm Addons project, which is licensed under the undefined license.
 * This code may be subject to personal license(s) used by axle.coffee or other third parties, please contact a Contributor for more information.
 *
 */
package noammaddons.commands.commands

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.command.ICommandSender
import noammaddons.commands.Command
import noammaddons.features.impl.misc.ClientTimer.clientTimerCommand
import noammaddons.features.impl.misc.ClientTimer.clientTimerCommandOnAllModes
import noammaddons.features.impl.misc.ClientTimer.clientTimerMode
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.sendChatMessage
import kotlin.concurrent.thread

/**
 * Handles the /timer command for scheduling actions after a delay.
 *
 * Usage:
 * /timer <time><unit>
 * Examples: /timer 1d, /timer 2h, /timer 30m, /timer 45s, /timer 1hour
 *
 * Modes:
 * - logout: Disconnects from the server and returns to the main menu.
 * - quit game: Shuts down the Minecraft client.
 * - run command: Executes a custom command after the timer.
 *
 * If called with no arguments and a timer is running, cancels the timer.
 */
object TimerCommand: Command("timer") {
    private var timerThread: Thread? = null

    /**
     * Disconnects the player from the current server and returns to the main menu.
     * We shedule this task to ensure it runs on the main thread which owns openGL or smth (idk ask google)
     */
    private fun logoutFromServer() {
        Minecraft.getMinecraft().addScheduledTask {
            Minecraft.getMinecraft().theWorld = null;
            Minecraft.getMinecraft().thePlayer = null;
            Minecraft.getMinecraft().displayGuiScreen(GuiMainMenu());
        };
    }

    /**
     * Shuts down the Minecraft client.
     */
    private fun quitGame() {
        Minecraft.getMinecraft().shutdown()
    }

    /**
     * Parses a time argument string into seconds.
     * Supports days, hours, minutes, seconds, and their full-word variants.
     *
     * @param input The time argument string.
     * @return The total time in seconds.
     */
    private fun parseTimeArg(input: String): Int {
        val units = arrayOf(
            Pair(Regex("""(\d+)\s*d(ays?)?""", RegexOption.IGNORE_CASE), 86400),
            Pair(Regex("""(\d+)\s*h(ours?)?""", RegexOption.IGNORE_CASE), 3600),
            Pair(Regex("""(\d+)\s*m(in(utes?)?)?""", RegexOption.IGNORE_CASE), 60),
            Pair(Regex("""(\d+)\s*s(ec(onds?)?)?""", RegexOption.IGNORE_CASE), 1),
        )
        for((regex, multiplier) in units) {
            val match = regex.find(input)
            if(match != null) {
                val value = match.groupValues[1].toIntOrNull() ?: 0
                return value * multiplier
            }
        }
        return input.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    /**
     * Processes the /timer command.
     * Cancels any running timer if called with no arguments.
     * Otherwise, starts a new timer for the specified duration and executes the selected action.
     *
     * @param sender The command sender.
     * @param args The command arguments.
     */
    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        if(args.isEmpty()) {
            if(timerThread?.isAlive == true) {
                timerThread?.interrupt()
                timerThread = null
                modMessage("&aTimer cancelled.")
            } else {
                modMessage("&cInvalid Usage. &bUsage: /timer <time><unit>")
            }
            return
        }
        val seconds = parseTimeArg(args.joinToString(" "))
        if(seconds <= 0) {
            modMessage("&cInvalid time. Please provide a positive duration.")
            return
        }
        modMessage("&aTimer set for $seconds seconds.")
        timerThread?.interrupt()
        timerThread = thread(start = true, isDaemon = true, name = "TimerCommandThread") {
            try {
                Thread.sleep(seconds * 1000L)
                val modes = arrayOf("logout", "quit game", "run command")
                val actions = arrayOf(
                    { logoutFromServer() },
                    { quitGame() },
                    { if(clientTimerCommand.isNotEmpty()) sendChatMessage(clientTimerCommand) },
                )
                if(clientTimerCommandOnAllModes && clientTimerCommand.isNotEmpty()) {
                    sendChatMessage(clientTimerCommand)
                }
                actions.getOrNull(clientTimerMode)?.invoke()
            } catch(e: InterruptedException) {
                modMessage("&cTimer interrupted due to an error or user action.")
            }
        }
    }
}