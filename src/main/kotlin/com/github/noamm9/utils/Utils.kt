package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import java.awt.Color
import java.awt.Desktop
import java.net.URI
import kotlin.reflect.KClass

object Utils {
    val favoriteColor = Color(0, 134, 255)

    fun openDiscordLink() {
        // to not false flag regex rat scanners
        Desktop.getDesktop().browse(URI("h*#t#t~p*s:/#/*d*is#c~o~r*d.~g~~*g#*/*p~j9*#m*QG~x#*M*#xB~".remove("#").remove("~").remove("*")))
    }

    fun Any?.equalsOneOf(vararg others: Any?): Boolean = others.any { this == it }

    fun Any?.containsOneOf(vararg elements: Any): Boolean = when (this) {
        is Collection<*> -> elements.any { contains(it) }
        is String -> elements.any { contains("$it") }
        is Array<*> -> elements.any { contains(it) }
        else -> false
    }

    fun Any?.isOneOf(vararg types: KClass<*>): Boolean {
        return types.any { it.isInstance(this) }
    }

    fun String.startsWithOneOf(vararg prefixes: String): Boolean = prefixes.any { startsWith(it) }
    fun String.endsWithOneOf(vararg suffixes: String): Boolean = suffixes.any { endsWith(it) }

    fun String.spaceCaps(): String {
        return this
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .replace(Regex("(?<=[A-Z])(?=[A-Z][a-z])"), " ")
            .trim()
    }

    fun String.uppercaseFirst() = this[0].uppercase() + substring(1)

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
        Exception().stackTrace.forEach { NoammAddons.logger.info(it.toString()) }
    }
}