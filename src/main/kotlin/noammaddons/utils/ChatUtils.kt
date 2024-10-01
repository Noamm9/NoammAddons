package noammaddons.utils


import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.DEBUG_PREFIX
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import gg.essential.universal.UChat
import kotlin.math.absoluteValue
import kotlin.math.sign
import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat.addColor
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import kotlin.math.log10
import kotlin.math.pow


object ChatUtils {
    fun getChatBreak(separator: String = "-"): String? {
        val len = mc.fontRendererObj.getStringWidth(separator)
        val times = mc.ingameGUI?.chatGUI?.chatWidth?.div(len)

        return times?.let { "-".repeat(it) }
    }

    fun Any?.equalsOneOf(vararg other: Any): Boolean = other.any { this == it }

    fun String?.removeFormatting(): String = UTextComponent.stripFormatting(this?.addColor() ?: "null")

    fun String.addColor(): String = addColor(this)

    fun modMessage(message: Any) = UChat.chat("$CHAT_PREFIX ${message.toString().addColor()}")

    fun debugMessage(message: Any) {
        if (config.DevMode) {
            UChat.chat("$DEBUG_PREFIX ${message.toString().addColor()}")
        }
    }

    fun sendChatMessage(message: Any) {
        mc.thePlayer?.sendChatMessage("$message") ?: return
    }


    /**
     * Sends a message to the user that they can click and run an action.
     *
     * @param message The message to be sent.
     * @param onClickCommand The command to be executed when the message is clicked.
     * @param hover The string to be shown when the message is hovered, default "Click here!".
     * @param prefix Whether to prefix the message with the chat prefix, default true.
     */
    fun clickableChat(
        message: String,
        onClickCommand: String,
        hover: String = "§eClick me!",
        prefix: Boolean = true,
    ) {
        val msgPrefix = if (prefix) CHAT_PREFIX else ""
        val textComponent = ChatComponentText(msgPrefix + message)
	    textComponent.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$onClickCommand")
		textComponent.chatStyle.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hover))
        mc.thePlayer.addChatMessage(textComponent)
    }



    fun Double.toFixed(digits: Int) = "%.${digits}f".format(this)

    fun formatNumber(num1: String): String {
        val num = num1.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: return "0"
        if (num == 0.0) return "0"

        val sign = num.sign
        val absNum = num.absoluteValue
	    
        if (absNum < 1) return "${if (sign == -1.0) "-" else ""}${"%.2f".format(absNum)}"

        val abbrev = listOf("", "k", "m", "b", "t", "q", "Q")
        val index = (log10(absNum) / 3).toInt().coerceIn(abbrev.indices)

        val formattedNumber = "${(sign * absNum / 10.0.pow((index * 3).toDouble())).toFixed(1)}${abbrev[index]}"

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
    fun showTitle(title: String = "", subtitle: String = "", time: Float = 3f) {
		if (title.isEmpty() && subtitle.isEmpty()) throw IllegalArgumentException("Both Title and subtitle cannot be empty")
        val gui = mc.ingameGUI ?: return
        val timeInTicks = (time * 20).toInt()
	    
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
        mc.thePlayer?.playSound("$MOD_ID:notificationsound", 1f, 1f) ?: return
    }

    data class Text(var text: String, var x: Double, var y: Double, var scale: Double)
}