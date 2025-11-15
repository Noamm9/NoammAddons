package noammaddons.utils

import gg.essential.universal.UChat
import gg.essential.universal.UDesktop
import kotlinx.serialization.json.*
import net.minecraft.network.Packet
import noammaddons.NoammAddons.Companion.CHAT_PREFIX
import noammaddons.NoammAddons.Companion.Logger
import noammaddons.NoammAddons.Companion.MOD_NAME
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.getCenteredText
import noammaddons.utils.ChatUtils.getChatBreak
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.ThreadUtils.setTimeout
import java.awt.Color
import java.net.URI
import kotlin.reflect.KClass


object Utils {
    interface INetworkManager {
        fun sendPacketNoEvent(packet: Packet<*>?)
    }

    val favoriteColor = Color(0, 134, 255)

    fun <T> splitArray(array: List<T>, size: Int): List<List<T>> {
        return array.chunked(size)
    }

    fun playFirstLoadMessage() {
        listOf(
            "&b&m${getChatBreak()?.substring(1)}",
            "§b§lThanks for installing $MOD_NAME§r §6§lForge!",
            "",
            "§dUse §b§l/no§lamma§lddons §r§eto access settings.",
            "§eAlias: §b§l/na.",
            "",
            "§dTo list all mod commands, Use §b§l/na help",
            "§aand lastly join my §9§ldiscord server",
            "§9§l${"h*#t#t~p*s:/#/*d*is#c~o~r*d.~g~~*g#*/*p~j9*#m*QG~x#*M*#xB~".remove("#").remove("~").remove("*")}",
            "&b&m${getChatBreak()?.substring(1)}"
        ).run {
            UChat.chat(joinToString("\n") { getCenteredText(it) })
        }
    }

    fun openDiscordLink() {
        // to not false flag regex rat scanners
        UDesktop.browse(URI("h*#t#t~p*s:/#/*d*is#c~o~r*d.~g~~*g#*/*p~j9*#m*QG~x#*M*#xB~".remove("#").remove("~").remove("*")))
    }


    fun Any?.equalsOneOf(vararg others: Any?): Boolean = others.any { this == it }

    fun Any?.containsOneOf(vararg elements: Any): Boolean = when (this) {
        is String -> elements.any { contains("$it") }
        is Collection<*> -> elements.any { contains(it) }
        is Array<*> -> elements.any { contains(it) }
        else -> false
    }

    fun Any?.isOneOf(vararg types: KClass<*>): Boolean {
        return types.any { it.isInstance(this) }
    }


    fun String.startsWithOneOf(vararg prefixes: String): Boolean = prefixes.any { startsWith(it) }
    fun String.spaceCaps(): String {
        return this
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .replace(Regex("(?<=[A-Z])(?=[A-Z][a-z])"), " ")
            .trim()
    }

    fun Packet<*>.send(delay: Number? = null) {
        if (delay == null || delay.toLong() <= 0) mc.netHandler.networkManager.sendPacket(this)
        else setTimeout(delay.toLong()) { mc.netHandler.networkManager.sendPacket(this) }
    }

    fun Packet<*>.sendNoEvent(delay: Number? = null) {
        val networkManager = mc.netHandler.networkManager as INetworkManager
        if (delay == null) networkManager.sendPacketNoEvent(this)
        else setTimeout(delay.toLong()) { networkManager.sendPacketNoEvent(this) }
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

    fun String.removeSpace() = replace("\\s".toRegex(), "")

    fun printCaller() {
        Exception().stackTrace.forEach { Logger.info(it) }
    }

    fun formatPbPuzzleMessage(puzzle: String, completionTime: Double, previousBest: Double?): String {
        val formattedTime = (completionTime / 1000.0).toFixed(2)
        val formattedBest = (((previousBest ?: 20000.0) / 1000.0)).toFixed(2)

        return buildString {
            append("$CHAT_PREFIX &b$puzzle Took: &d")
            if (previousBest != null && completionTime <= previousBest) {
                val data = personalBests.getData()
                data.pazzles[puzzle] = completionTime
                personalBests.setData(data)
                personalBests.save()

                append("${formattedTime}s &d&lPB! &7(${formattedBest}s)")
            }
            else if (previousBest != null) append("${formattedTime}s &7(${formattedBest}s)")
            else {
                val data = personalBests.getData()
                data.pazzles[puzzle] = completionTime
                personalBests.setData(data)
                personalBests.save()
                append("${formattedTime}s")
            }
        }.addColor()
    }

    fun JsonElement.toGson(): com.google.gson.JsonElement {
        return when (this) {
            is JsonObject -> com.google.gson.JsonObject().apply {
                this@toGson.forEach { (key, value) ->
                    add(key, value.toGson())
                }
            }

            is JsonArray -> com.google.gson.JsonArray().apply {
                this@toGson.forEach { element ->
                    add(element.toGson())
                }
            }

            is JsonPrimitive -> when {
                this.isString -> com.google.gson.JsonPrimitive(this.content)
                this.booleanOrNull != null -> com.google.gson.JsonPrimitive(this.boolean)
                this.intOrNull != null -> com.google.gson.JsonPrimitive(this.int)
                this.longOrNull != null -> com.google.gson.JsonPrimitive(this.long)
                this.doubleOrNull != null -> com.google.gson.JsonPrimitive(this.double)
                else -> com.google.gson.JsonNull.INSTANCE
            }

            JsonNull -> com.google.gson.JsonNull.INSTANCE
            else -> com.google.gson.JsonNull.INSTANCE
        }
    }

}