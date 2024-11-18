package noammaddons.utils

import gg.essential.universal.UChat
import noammaddons.utils.SoundUtils.chipiChapa


object Utils {
    fun Any?.isNull(): Boolean = this == null

    fun <T> splitArray(array: List<T>, size: Int): List<List<T>> {
        return array.chunked(size)
    }

    fun playFirstLoadMessage() {
        chipiChapa.start()
        val centeredTexts = listOf(
            "§b§lThanks for installing NoammAddons§r §6§lForge!",
            "",
            "§dUse §b§l/no§lamma§lddons §r§eto access settings.",
            "§eAlias: §b§l/na.",
            "",
            "§dTo list all mod commands, Use §b§l/na help",
            "§aand lastly join my §9§ldiscord server",
            "§9§lhttps://discord.gg/pj9mQGxMxB"
        )

        UChat.chat(
            """&b&m${ChatUtils.getChatBreak()?.substring(1)}
${centeredTexts.joinToString("\n") { ChatUtils.getCenteredText(it) }}
&b&m${ChatUtils.getChatBreak()?.substring(1)}""".trim().trimIndent()
        )
    }


    fun Any?.equalsOneOf(vararg other: Any): Boolean = other.any { this == it }

    fun Any?.containsOneOf(vararg other: Any): Boolean {
        return when (this) {
            is String -> other.any { this.contains("$it") }
            is Collection<*> -> other.any { this.contains(it) }
            is Array<*> -> other.any { this.contains(it) }
            else -> false
        }
    }
}