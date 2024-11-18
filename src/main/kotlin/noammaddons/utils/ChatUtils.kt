package noammaddons.utils


import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat
import gg.essential.universal.UChat.addColor
import gg.essential.universal.UGraphics.getStringWidth
import gg.essential.universal.wrappers.message.UTextComponent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.ChatAllowedCharacters.filterAllowedCharacters
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.PacketEvent
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.DEBUG_PREFIX
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.scope
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PartyUtils.isInParty
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils.drawTitle
import noammaddons.utils.SoundUtils.notificationSound
import noammaddons.utils.ThreadUtils.loop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.math.*


object ChatUtils {
    private val abbrev = listOf("", "k", "m", "b", "t", "q", "Q")
    private val numbersOnlyRegex = Regex("[^0-9.]")

    private var titleText = ""
    private var subtitleText = ""
    private var time = 0L
    private var rainbow = false

    private var startTime = 0L
    private var ping = 0
    private var isCalculatingPing = false


    fun getChatBreak(separator: String = "-"): String? {
        val len = getStringWidth(separator)
        val times = mc.ingameGUI?.chatGUI?.chatWidth?.div(len)

        return times?.let { "-".repeat(it) }
    }

    fun sendFakeChatMessage(message: String) {
        val formattedMessage = message.addColor()
        modMessage(formattedMessage)
        MinecraftForge.EVENT_BUS.post(ClientChatReceivedEvent(0.toByte(), ChatComponentText(formattedMessage)))
        MinecraftForge.EVENT_BUS.post(Chat(ChatComponentText(formattedMessage)))
        MinecraftForge.EVENT_BUS.post(PacketEvent.Received(S02PacketChat(ChatComponentText(formattedMessage), 0.toByte())))
    }

    fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }

    fun getCenteredText(text: String): String {
        val textWidth = getStringWidth(text.addColor())
        val chatWidth = mc.ingameGUI?.chatGUI?.chatWidth ?: 0

        if (textWidth >= chatWidth) return text

        val spaceWidth = (chatWidth - textWidth) / 2f
        val spaceBuilder = StringBuilder().apply {
            repeat((spaceWidth / getStringWidth(" ")).roundToInt()) {
                append(' ')
            }
        }

        return spaceBuilder.append(text).toString()
    }

    fun removeUnicode(input: String): String {
        return input.replace(Regex("[^\\u0000-\\u007F]"), "")
    }

    fun String?.removeFormatting(): String = UTextComponent.stripFormatting(this?.addColor() ?: "null")

    fun String.addColor(): String = addColor(this)

    fun modMessage(message: Any?) = UChat.chat("$CHAT_PREFIX ${message.toString().addColor()}")

    fun debugMessage(message: Any) {
        if (config.DevMode) UChat.chat("$DEBUG_PREFIX ${message.toString().addColor()}")
    }

    fun sendChatMessage(message: Any) {
        Player?.sendChatMessage(filterAllowedCharacters("$message"))
    }

    fun sendPartyMessage(message: Any) {
        if (isInParty()) sendChatMessage("/pc $message")
    }

    /**
     * Sends a message to the user that they can click and run an action.
     *
     * @param msg The message to be sent.
     * @param cmd The command to be executed when the message is clicked.
     * @param hover The string to be shown when the message is hovered, default "Click here!".
     * @param prefix Whether to prefix the message with the chat prefix, default true.
     */
    fun clickableChat(msg: String, cmd: String = "", hover: String = "§eClick me!", prefix: Boolean = true) {
        val msgPrefix = if (prefix) "$CHAT_PREFIX " else ""
        val textComponent = ChatComponentText(msgPrefix + msg.addColor())

        val command = if (cmd.startsWith("/")) cmd.addColor() else "/${cmd.addColor()}"

        textComponent.chatStyle = ChatStyle().apply {
            if (cmd.isNotBlank()) chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
            chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hover.addColor()))
        }

        Player?.addChatMessage(textComponent)
    }

    fun formatNumber(num1: String): String {
        val num = num1.replace(numbersOnlyRegex, "").toDoubleOrNull() ?: return "0"
        if (num == 0.0) return "0"

        val absNum = num.absoluteValue
        val sign = num.sign

        if (absNum < 1) return "${if (sign == - 1.0) "-" else ""}${"%.2f".format(absNum)}"

        val index = (log10(absNum) / 3).toInt().coerceIn(abbrev.indices)
        val abbreviatedValue = absNum / 10.0.pow((index * 3).toDouble())
        val formattedNumber = "${"%.1f".format(abbreviatedValue)}${abbrev[index]}"

        return if (sign == - 1.0) "-$formattedNumber" else formattedNumber
    }

    fun addRandomColorCodes(inputString: String): String {
        val colorCodes = listOf("§6", "§c", "§e", "§f")
        val result = StringBuilder()
        var lastColor: String? = null

        for (char in inputString.removeFormatting()) {
            val availableColors = colorCodes.filter { it != lastColor }
            val randomColor = availableColors.random()
            result.append(randomColor).append(char).append("§r")
            lastColor = randomColor
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
    @Synchronized
    fun showTitle(title: String = "", subtitle: String = "", time: Number = 3f, rainbow: Boolean = false) {
        if (title.isBlank() && subtitle.isBlank()) throw IllegalArgumentException("Both Title and subtitle cannot be empty")

        this.titleText = title
        this.subtitleText = subtitle
        this.time = time.toLong() * 1000L
        this.rainbow = rainbow
    }

    init {
        loop(100) { time -= 100 }
    }

    @SubscribeEvent
    fun renderTitles(event: RenderOverlay) {
        if (time <= 0) return

        drawTitle(titleText, subtitleText, rainbow)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getPing(callback: (ping: Int) -> Unit) {
        if (isCalculatingPing) return // Prevent multiple ping checks simultaneously
        if (! inSkyblock) return

        isCalculatingPing = true
        startTime = System.currentTimeMillis()
        sendChatMessage("/Noamm9 is the best!")

        scope.launch {
            while (isCalculatingPing) {
                delay(1)
            }
            isCalculatingPing = false
            callback(ping)
        }
    }

    @SubscribeEvent
    fun calcPing(event: Chat) {
        if (! event.component.unformattedText.removeFormatting().contains("('Noamm9 is the best!')")) return
        event.isCanceled = true

        ping = (System.currentTimeMillis() - startTime).toInt()
        isCalculatingPing = false
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
    fun Alert(title: String = FULL_PREFIX, message: String, duration: Int = 3, clickFunction: () -> Unit = {}, closeFunction: () -> Unit = {}) {
        EssentialAPI.getNotifications().push(
            title.addColor(),
            message.addColor(),
            (if (duration == - 1) 999999999999999999 else duration).toFloat(),
            clickFunction,
            closeFunction
        )
        notificationSound.start()
    }
}