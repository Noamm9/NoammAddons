package com.github.noamm9.features.impl.dev.text

import com.github.noamm9.NoammAddons.mc
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.resources.RegistryOps
import net.minecraft.util.FormattedCharSequence
import java.util.*
import java.util.regex.Pattern

object TextReplacer {
    private class Replacement(val target: String, val component: Component, val plainString: String)

    private val unchanged = Any()
    private val hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val stringCache = Collections.synchronizedMap(newBoundedCache<String, String>(2048))
    private val literalCache = Collections.synchronizedMap(newBoundedCache<Style, MutableMap<String, Any>>(64))

    @Volatile
    private var replacementVersion = - 1

    @Volatile
    private var preparedReplacements = emptyList<Replacement>()

    private val replacementMap = ReplacementMap(::clearCache)

    fun setCustomReplacements(replacements: Map<String, String>) {
        replacementMap.clear()
        replacementMap.putAll(replacements)
    }

    @JvmStatic
    fun handleString(text: String): String {
        if (text.isEmpty()) return text
        val replacements = getCache()
        if (replacements.isEmpty()) return text

        return synchronized(stringCache) {
            stringCache.getOrPut(text) { replaceText(text, replacements) ?: text }
        }
    }

    @JvmStatic
    fun handleComponent(component: Component): Component {
        val replacements = getCache()
        if (replacements.isEmpty()) return component

        return rebuildComponent(component, replacements) ?: component
    }

    @JvmStatic
    fun handleCharSequence(seq: FormattedCharSequence): FormattedCharSequence {
        val replacements = getCache()
        if (replacements.isEmpty()) return seq

        val styles = ArrayList<Style>()
        val texts = ArrayList<String>()
        var currentStyle = Style.EMPTY
        val currentText = StringBuilder()
        var hasMatch = false

        fun flush() {
            if (currentText.isEmpty()) return

            val text = currentText.toString()
            if (! hasMatch && containsTarget(text, replacements)) hasMatch = true
            styles.add(currentStyle)
            texts.add(text)
            currentText.setLength(0)
        }

        seq.accept { _, style, codePoint ->
            if (style != currentStyle) {
                flush()
                currentStyle = style
            }

            currentText.appendCodePoint(codePoint)
            true
        }

        flush()

        if (! hasMatch) return seq

        val rebuilt = Component.empty()
        for (i in texts.indices) {
            val text = texts[i]
            val style = styles[i]
            rebuilt.append(replaceLiteral(text, style, replacements) ?: Component.literal(text).withStyle(style))
        }

        return rebuilt.visualOrderText
    }

    private fun rebuildComponent(component: Component, replacements: List<Replacement>): MutableComponent? {
        val replacedLiteral = (component.contents as? PlainTextContents)?.let {
            replaceLiteral(it.text(), component.style, replacements)
        }

        var siblingsChanged = false
        val siblings = if (component.siblings.isEmpty()) null else ArrayList<Component>(component.siblings.size)

        for (sibling in component.siblings) {
            val rebuilt = rebuildComponent(sibling, replacements)
            if (rebuilt != null) siblingsChanged = true
            siblings?.add(rebuilt ?: sibling)
        }

        if (replacedLiteral == null && ! siblingsChanged) return null

        val result = replacedLiteral ?: component.plainCopy().apply { style = component.style }
        if (result.style.isEmpty) result.style = component.style
        siblings?.forEach(result::append)
        return result
    }

    private fun replaceLiteral(text: String, parentStyle: Style, replacements: List<Replacement>): MutableComponent? {
        val styleCache = synchronized(literalCache) {
            literalCache.getOrPut(parentStyle) {
                Collections.synchronizedMap(newBoundedCache<String, Any>(128))
            }
        }

        val cached = synchronized(styleCache) {
            styleCache.getOrPut(text) { buildLiteralReplacement(text, parentStyle, replacements) ?: unchanged }
        }

        return when (cached) {
            is MutableComponent -> cached.copy()
            else -> null
        }
    }

    private fun buildLiteralReplacement(text: String, parentStyle: Style, replacements: List<Replacement>): MutableComponent? {
        var cursor = 0
        var rebuilt: MutableComponent? = null

        while (cursor < text.length) {
            var bestIndex = Int.MAX_VALUE
            var bestReplacement: Replacement? = null

            for (replacement in replacements) {
                val index = text.indexOf(replacement.target, cursor)
                if (index == - 1 || index >= bestIndex) continue

                bestIndex = index
                bestReplacement = replacement

                if (index == cursor) break
            }

            val match = bestReplacement ?: break
            val result = rebuilt ?: Component.empty().also { rebuilt = it }

            if (bestIndex > cursor) {
                result.append(Component.literal(text.substring(cursor, bestIndex)).withStyle(parentStyle))
            }

            val replacementComp = match.component.copy()
            if (replacementComp.style.isEmpty) replacementComp.withStyle(parentStyle)
            result.append(replacementComp)

            cursor = bestIndex + match.target.length
        }

        val result = rebuilt ?: return null
        if (cursor < text.length) result.append(Component.literal(text.substring(cursor)).withStyle(parentStyle))
        if (result.style.isEmpty) result.style = parentStyle
        return result
    }

    private fun replaceText(text: String, replacements: List<Replacement>): String? {
        var cursor = 0
        var rebuilt: StringBuilder? = null

        while (cursor < text.length) {
            var bestIndex = Int.MAX_VALUE
            var bestReplacement: Replacement? = null

            for (replacement in replacements) {
                val index = text.indexOf(replacement.target, cursor)
                if (index == - 1 || index >= bestIndex) continue

                bestIndex = index
                bestReplacement = replacement

                if (index == cursor) break
            }

            val match = bestReplacement ?: break
            val result = rebuilt ?: StringBuilder(text.length).also { rebuilt = it }

            if (bestIndex > cursor) {
                result.append(text, cursor, bestIndex)
            }

            result.append(match.plainString)
            cursor = bestIndex + match.target.length
        }

        val result = rebuilt ?: return null
        if (cursor < text.length) result.append(text, cursor, text.length)
        return result.toString()
    }

    private fun containsTarget(text: String, replacements: List<Replacement>): Boolean {
        for (replacement in replacements) {
            if (text.indexOf(replacement.target) != - 1) {
                return true
            }
        }
        return false
    }

    private fun getCache(): List<Replacement> {
        val version = replacementMap.version
        if (replacementVersion == version) return preparedReplacements

        val replacements = replacementMap.entries.asSequence()
            .map { (target, replacement) ->
                val component = parseJsonToComponent(replacement) ?: Component.literal(applyColorCodes(replacement))
                Replacement(target, component, applyColorCodes(component.string))
            }
            .sortedWith(compareByDescending<Replacement> { it.target.length }.thenBy { it.target })
            .toList()

        preparedReplacements = replacements
        replacementVersion = version
        return replacements
    }

    private fun parseJsonToComponent(json: String): MutableComponent? {
        if (! json.trim().startsWith("{") && ! json.trim().startsWith("[")) return null

        try {
            val jsonElement = JsonParser.parseString(json)
            val registry = mc.level?.registryAccess() ?: mc.connection?.registryAccess()

            val ops = if (registry != null) RegistryOps.create(JsonOps.INSTANCE, registry) else JsonOps.INSTANCE
            return ComponentSerialization.CODEC.parse(ops, jsonElement).result().orElse(null)?.copy()
        }
        catch (_: Exception) {
            return null
        }
    }

    private fun applyColorCodes(text: String): String {
        if ('&' !in text) return text

        val matcher = hexPattern.matcher(text)
        val buffer = StringBuffer()
        while (matcher.find()) {
            val hex = matcher.group(1)
            val replacement = StringBuilder("\u00A7x")
            for (char in hex) replacement.append("\u00A7").append(char)
            matcher.appendReplacement(buffer, replacement.toString())
        }
        matcher.appendTail(buffer)
        return buffer.toString().replace("&", "\u00A7")
    }

    private fun <K, V> newBoundedCache(maxEntries: Int): LinkedHashMap<K, V> {
        return object: LinkedHashMap<K, V>(maxEntries + 1, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
                return size > maxEntries
            }
        }
    }

    private fun clearCache() {
        synchronized(stringCache) { stringCache.clear() }
        synchronized(literalCache) { literalCache.clear() }
        preparedReplacements = emptyList()
        replacementVersion = - 1
    }
}