package noammaddons.utils

import gg.essential.universal.UChat
import gg.essential.universal.UDesktop
import net.minecraft.network.Packet
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.Logger
import noammaddons.noammaddons.Companion.MOD_NAME
import noammaddons.noammaddons.Companion.mc
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.getCenteredText
import noammaddons.utils.ChatUtils.getChatBreak
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.ThreadUtils.setTimeout
import java.awt.Color
import java.net.URI


object Utils {
    val favoriteColor = Color(0, 134, 255)

    fun <T> splitArray(array: List<T>, size: Int): List<List<T>> {
        return array.chunked(size)
    }

    fun playFirstLoadMessage() {
        SoundUtils.chipiChapa()
        listOf(
            "&b&m${getChatBreak()?.substring(1)}",
            "§b§lThanks for installing $MOD_NAME§r §6§lForge!",
            "",
            "§dUse §b§l/no§lamma§lddons §r§eto access settings.",
            "§eAlias: §b§l/na.",
            "",
            "§dTo list all mod commands, Use §b§l/na help",
            "§aand lastly join my §9§ldiscord server",
            "§9§lhttps://discord.gg/pj9mQGxMxB",
            "&b&m${getChatBreak()?.substring(1)}"
        ).run {
            UChat.chat(joinToString("\n") { getCenteredText(it) })
        }
    }

    fun openDiscordLink() {
        UDesktop.browse(URI("https://discord.gg/pj9mQGxMxB"))
    }


    fun Any?.equalsOneOf(vararg others: Any?): Boolean = others.any { this == it }

    fun Any?.containsOneOf(vararg elements: Any): Boolean = when (this) {
        is String -> elements.any { contains("$it") }
        is Collection<*> -> elements.any { contains(it) }
        is Array<*> -> elements.any { contains(it) }
        else -> false
    }

    fun String.startsWithOneOf(vararg prefixes: String): Boolean = prefixes.any { startsWith(it) }
    fun String.spaceCaps(): String {
        return this
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .replace(Regex("(?<=[A-Z])(?=[A-Z][a-z])"), " ")
            .trim()
    }

    fun Packet<*>.send(delay: Number? = null) {
        if (delay == null) mc.netHandler.networkManager.sendPacket(this)
        else setTimeout(delay.toLong()) { mc.netHandler.networkManager.sendPacket(this) }
    }

    fun <K, V> MutableMap<K, V>.removeIf(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
        var removed = false
        val iterator = this.entries.iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next())) {
                iterator.remove()
                removed = true
            }
        }
        return removed
    }

    fun String.remove(vararg patterns: String): String = patterns.fold(this) { acc, s -> acc.replace(s, "") }
    fun String.remove(vararg patterns: Regex): String = patterns.fold(this) { acc, r -> acc.replace(r, "") }
    fun String.remove(vararg patterns: Any): String =
        patterns.fold(this) { acc, p ->
            when (p) {
                is String -> acc.replace(p, "")
                is Regex -> acc.replace(p, "")
                else -> acc
            }
        }


    fun printCaller() {
        Exception().stackTrace.forEach { Logger.info(it) }
    }

    fun formatPbPuzzleMessage(puzzle: String, completionTime: Double, previousBest: Double?): String {
        val pb = previousBest ?: Double.MAX_VALUE

        val formattedTime = (completionTime / 1000.0).toFixed(2)
        val formattedBest = (pb / 1000.0).toFixed(2)

        return buildString {
            append("$CHAT_PREFIX &b$puzzle Done: &d")
            if (completionTime <= pb) {
                val data = personalBests.getData()
                data.pazzles[puzzle] = completionTime
                personalBests.setData(data)
                personalBests.save()

                append("${formattedTime}s &d&lPB! &7(${formattedBest}s)")
            }
            else append("${formattedTime}s &7(${formattedBest}s)")
        }.addColor()
    }
}