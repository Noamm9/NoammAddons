package NoammAddons.utils


import NoammAddons.NoammAddons.Companion.CHAT_PREFIX
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.sounds.notificationsound
import gg.essential.universal.UChat
import kotlin.math.absoluteValue
import kotlin.math.sign
import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat.addColor
import gg.essential.universal.wrappers.message.UTextComponent


object ChatUtils {
    fun getChatBreak(separator: String = "-"): String? {
        val len = mc.fontRendererObj.getStringWidth(separator)
        val times = mc.ingameGUI?.chatGUI?.chatWidth?.div(len)

        return times?.let { "-".repeat(it) }
    }

    fun Any?.equalsOneOf(vararg other: Any): Boolean = other.any { this == it }

    fun String?.removeFormatting(): String = UTextComponent.stripFormatting(this ?: "")

    fun String.addColor(): String = addColor(this)

    fun modMessage(message: Any) = UChat.chat("$CHAT_PREFIX ${message.toString().addColor()}")

    fun sendChatMessage(message: Any) {
        mc.thePlayer.sendChatMessage("$message")
    }

    fun Double.toFixed(digits: Int) = "%.${digits}f".format(this)

    fun formatNumber(num1: String): String {
        val num = num1.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: return "0"
        if (num == 0.0) return "0"

        val sign = num.sign
        val absNum = num.absoluteValue

        // Handle numbers less than 1
        if (absNum < 1) return "${if (sign == -1.0) "-" else ""}${"%.2f".format(absNum)}"

        val abbrev = listOf("", "k", "m", "b", "t", "q", "Q")
        val index = (Math.log10(absNum) / 3).toInt().coerceIn(abbrev.indices)

        val formattedNumber = "${(sign * absNum / Math.pow(10.0, (index * 3).toDouble())).toFixed(1)}${abbrev[index]}"

        return formattedNumber
    }

    fun addRandomColorCodes(inputString: String): String {
        val colorCodes = listOf("§6", "§c", "§e", "§f")
        val result = StringBuilder()

        for (char in inputString) {
            val randomColor = colorCodes.random()
            result.append(randomColor).append(char).append("§r")
        }

        return result.toString()
    }

    /**
     * Display a title.
     *
     * @param title title text
     * @param subtitle subtitle text
     * @param time time to stay on screen in seconds
     */
    fun showTitle(title: String, subtitle: String = "", time: Int = 3) {
        val gui = mc.ingameGUI
        val timeInTicks = time * 20
        gui.displayTitle(null, null, 0, timeInTicks, 0)
        gui.displayTitle(title.addColor(), null, 0, timeInTicks, 0)
        gui.displayTitle(null, subtitle.addColor(), 0, timeInTicks, 0)
        gui.displayTitle(null, null, 0, timeInTicks, 0)
    }


    /**
     * Displays a notification with a custom message and duration.
     *
     * @param title The title of the notification.
     * @param message The message to display in the notification.
     * @param duration The duration in seconds for which the notification should be displayed. Defaults to 3 seconds.
     * @param clickFunction The function to be executed when the notification is clicked. Defaults to an empty function.
     * @param closeFunction The function to be executed when the notification is closed. Defaults to an empty function.
     */
    fun Alert(title: String, message: String, duration: Int = 3, clickFunction: () -> Unit = {}, closeFunction: () -> Unit = {}) {
        EssentialAPI.getNotifications().push(
            title,
            message,
            duration.toFloat(),
            clickFunction,
            closeFunction
        )
        notificationsound.play()
    }

    data class Text(var text: String, var x: Double, var y: Double, var scale: Double)
}