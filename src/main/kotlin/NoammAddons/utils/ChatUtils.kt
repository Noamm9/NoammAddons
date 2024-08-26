package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.CHAT_PREFIX
import NoammAddons.NoammAddons.Companion.mc
import gg.essential.universal.UChat
import kotlin.math.absoluteValue
import kotlin.math.sign

object ChatUtils {
    fun getChatBreak(separator: String = "-"): String? {
        val len = mc.fontRendererObj.getStringWidth(separator)
        val times = mc.ingameGUI?.chatGUI?.chatWidth?.div(len)

        return times?.let { "-".repeat(it) }
    }

    fun Any?.equalsOneOf(vararg other: Any): Boolean = other.any { this == it }

    fun String.removeFormatting(): String = this.replace("[\u00a7&][0-9a-fk-or]".toRegex(), "")

    fun String.addColor(): String = this.replace("(?<!\\\\)&(?![^0-9a-fk-or]|$)".toRegex(), "\u00a7")

    fun modMessage(message: Any) = UChat.chat("$CHAT_PREFIX $message")

    fun Double.toFixed(digits: Int) = "%.${digits}f".format(this)

    fun formatNumber(num1: String): String {
        val num = num1.replace(",", "").toDoubleOrNull() ?: return "0"
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

    fun showTitle(title: String?, subtitle: String? = null, time: Int = 3000) {
        mc.ingameGUI.displayTitle(title?.addColor(), subtitle, 50, time, 100)
    }

}