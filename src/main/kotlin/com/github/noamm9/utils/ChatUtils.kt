package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.utils.Utils.remove
import com.github.noamm9.utils.render.Render2D
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket
import net.minecraft.network.protocol.game.ServerboundChatPacket
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

object ChatUtils {
    private val queue = ConcurrentLinkedQueue<String>()
    private val isProcessing = AtomicBoolean(false)

    @Volatile
    private var lastSentTime = 0L

    fun init() {
        EventBus.register<PacketEvent.Sent> {
            if (event.packet !is ServerboundChatPacket &&
                event.packet !is ServerboundChatCommandPacket &&
                event.packet !is ServerboundChatCommandSignedPacket
            ) return@register

            lastSentTime = System.currentTimeMillis()
        }
    }

    private fun process() {
        if (! isProcessing.compareAndSet(false, true)) return

        scope.launch {
            try {
                while (queue.isNotEmpty()) {
                    val str = queue.poll()?.removeFormatting() ?: break
                    val waitTime = 300L - System.currentTimeMillis() - lastSentTime
                    if (waitTime > 0) delay(waitTime)

                    mc.execute {
                        val conn = mc.player?.connection ?: return@execute
                        if (str.startsWith("/")) conn.sendCommand(str.removePrefix("/"))
                        else conn.sendChat(str)
                    }

                    lastSentTime = System.currentTimeMillis()
                }
            }
            finally {
                isProcessing.set(false)
                if (queue.isNotEmpty()) process()
            }
        }
    }

    fun sendMessage(message: String) {
        queue.add(message)
        process()
    }

    fun sendCommand(command: String) {
        queue.add("/" + command.removePrefix("/"))
        process()
    }

    val formattingRegex = "(?i)[§&]([0-9a-fk-or]|x[0-9a-f]{6})".toRegex()
    fun String.removeFormatting() = remove(formattingRegex)

    fun modMessage(msg: Any?) = chat("${NoammAddons.PREFIX} $msg")

    fun sendPartyMessage(msg: Any?) {
        if (! PartyUtils.isInParty) return
        sendMessage("/pc $msg")
    }

    fun chat(msg: Any?) = ThreadUtils.runOnMcThread { mc.gui?.chat?.addMessage(Component.literal(msg.toString().addColor())) }
    fun chat(comp: Component) = ThreadUtils.runOnMcThread { mc.gui?.chat?.addMessage(comp) }

    fun String.addColor() = replace("&".toRegex(), "§")

    val Component.unformattedText: String get() = this.string.removeFormatting()
    val Component.formattedText: String
        get() {
            val sb = StringBuilder()

            visit({ style, string ->
                style.color?.let { textColor ->
                    val colorMatch = ChatFormatting.entries.firstOrNull {
                        it.isColor && it.color == textColor.value
                    }

                    if (colorMatch != null) {
                        sb.append("§${colorMatch.char}")
                    }
                }

                if (style.isBold) sb.append("§${ChatFormatting.BOLD.char}")
                if (style.isItalic) sb.append("§${ChatFormatting.ITALIC.char}")
                if (style.isUnderlined) sb.append("§${ChatFormatting.UNDERLINE.char}")
                if (style.isStrikethrough) sb.append("§${ChatFormatting.STRIKETHROUGH.char}")
                if (style.isObfuscated) sb.append("§${ChatFormatting.OBFUSCATED.char}")

                sb.append(string)

                Optional.empty<String>()
            }, Style.EMPTY)

            return sb.toString()
        }

    fun getChatBreak(): String {
        val chatWidth = mc.gui?.chat?.width ?: return ""
        val textRenderer = mc.font
        val dashWidth = textRenderer.width("-")

        val repeatCount = chatWidth / dashWidth
        return "-".repeat(repeatCount)
    }

    fun getCenteredText(text: String): String {
        val chatWidth = mc.gui?.chat?.width ?: return text
        val textRenderer = mc.font
        val textWidth = textRenderer.width(text)
        if (textWidth >= chatWidth) return text
        val spaceWidth = textRenderer.width(" ")

        val padding = ((chatWidth - textWidth) / 2f / spaceWidth).roundToInt()
        return " ".repeat(padding) + text
    }

    private var title = ""
    private var subtitle = ""
    private var time = 0


    fun showTitle(title: Any? = "", subtitle: Any? = "") {
        this.title = title.toString()
        this.subtitle = subtitle.toString()
        this.time = 40
    }

    init {
        register<TickEvent.Start> { if (time > 0) time -- }

        register<RenderOverlayEvent>(EventPriority.LOW) {
            if (time > 0) {
                Render2D.drawCenteredString(event.context, title, mc.window.guiScaledWidth / 2f, mc.window.guiScaledHeight / 2f - 50f, scale = 2.5)
                Render2D.drawCenteredString(event.context, subtitle, mc.window.guiScaledWidth / 2f, mc.window.guiScaledHeight / 2f + 20f, scale = 1.5)
            }
        }
    }
}