package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.NoammAddons.scope
import com.github.noamm9.event.EventBus.register
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.utils.render.Render2D
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
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
        register<PacketEvent.Sent> {
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

    fun String.removeFormatting(): String {
        if (this.isEmpty()) return ""

        val len = this.length
        val out = CharArray(len)
        var outPos = 0
        var i = 0

        while (i < len) {
            val c = this[i]
            var skipped = false

            if ((c == '§' || c == '&') && i + 1 < len) {
                val next = this[i + 1]

                if ((next == 'x' || next == 'X') && i + 7 < len) {
                    var isHexSequence = true
                    for (k in 2 .. 7) {
                        val h = this[i + k]
                        if (! ((h in '0' .. '9') || (h in 'a' .. 'f') || (h in 'A' .. 'F'))) {
                            isHexSequence = false
                            break
                        }
                    }

                    if (isHexSequence) {
                        i += 8
                        skipped = true
                    }
                }

                if (! skipped) {
                    if ((next in '0' .. '9') ||
                        (next in 'a' .. 'f') || (next in 'A' .. 'F') ||
                        (next in 'k' .. 'o') || (next in 'K' .. 'O') ||
                        next == 'r' || next == 'R'
                    ) {
                        i += 2
                        skipped = true
                    }
                }
            }

            if (! skipped) {
                out[outPos ++] = c
                i ++
            }
        }

        return String(out, 0, outPos)
    }

    fun modMessage(msg: Any?) = chat("${NoammAddons.PREFIX} $msg")

    fun sendPartyMessage(msg: Any?) {
        if (! PartyUtils.isInParty) return
        sendMessage("/pc $msg")
    }

    fun chat(msg: Any?) = ThreadUtils.runOnMcThread { mc.gui?.chat?.addMessage(Component.literal(msg.toString().addColor())) }
    fun chat(comp: Component) = ThreadUtils.runOnMcThread { mc.gui?.chat?.addMessage(comp) }

    fun String.addColor() = replace("&", "§")

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

    private var title = ""
    private var subtitle = ""
    private var time = 0


    fun showTitle(title: Any? = "", subtitle: Any? = "") {
        this.title = title.toString()
        this.subtitle = subtitle.toString()
        this.time = 40
    }

    fun clickableChat(
        message: String,
        prefix: Boolean = false,
        command: String? = null,
        hover: String? = null,
        copy: String? = null
    ) {
        if (mc.player == null) return
        val mainComponent = Component.literal(message.addColor())
        var style = Style.EMPTY

        if (hover != null) style = style.withHoverEvent(HoverEvent.ShowText(Component.literal(hover.addColor())))
        if (command != null) style = style.withClickEvent(ClickEvent.RunCommand(command))
        else if (copy != null) style = style.withClickEvent(ClickEvent.CopyToClipboard(copy))

        mainComponent.style = style

        ChatUtils.chat(if (prefix) Component.literal(NoammAddons.PREFIX + " ").append(mainComponent) else mainComponent)
    }

    init {
        register<TickEvent.Start> { if (time > 0) time -- }

        register<RenderOverlayEvent>(EventPriority.LOW) {
            if (time > 0) {
                val x = mc.window.guiScaledWidth / 2f
                val height = mc.window.guiScaledHeight
                val y = height / 2f - (height * 0.056).roundToInt()

                Render2D.drawCenteredString(event.context, title, x, y, scale = 2.5)
                Render2D.drawCenteredString(event.context, subtitle, x, y + (height / 15.42f).roundToInt(), scale = 1.5)
            }
        }
    }
}