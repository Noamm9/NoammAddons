package com.github.noamm9.features.impl.dev.text

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.addColor
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.RegistryOps
import net.minecraft.util.FormattedCharSequence
import java.util.*
import java.util.regex.Pattern

object TextReplacer {
    private data class Match(val startIndex: Int, val replacement: Replacement)

    private val hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val stringCache = Collections.synchronizedMap(newBoundedCache<String, String>(2048))
    private val literalCache = Collections.synchronizedMap(newBoundedCache<Style, MutableMap<String, Any>>(64))

    @Volatile
    private var replacementVersion = - 1

    @Volatile
    private var preparedReplacements = emptyList<Replacement>()

    private val replacementMap = ReplacementMap(::clearCache)

    private var ahoCorasick: AhoCorasick = AhoCorasick(listOf())

    fun setCustomReplacements(replacements: Map<String, String>) {
        replacementMap.clear()
        replacementMap.putAll(replacements)
        ahoCorasick = AhoCorasick(getCache())
    }

    @JvmStatic
    fun handleString(text: String): String {
        if (text.isEmpty()) return text
        val replacements = getCache()
        if (replacements.isEmpty()) return text

        return synchronized(stringCache) {
            stringCache.getOrPut(text) { replaceText(text) ?: text }
        }
    }

    @JvmStatic
    fun handleCharSequence(seq: FormattedCharSequence): FormattedCharSequence {
        val replacements = getCache()
        if (replacements.isEmpty()) return seq

        val styles = ArrayList<Style>()
        val texts = ArrayList<String>()
        var currentStyle = Style.EMPTY
        val currentText = StringBuilder()

        fun flush() {
            if (currentText.isEmpty()) return
            styles.add(currentStyle)
            texts.add(currentText.toString())
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

        val segmentOffsets = IntArray(texts.size + 1)
        for (i in texts.indices) segmentOffsets[i + 1] = segmentOffsets[i] + texts[i].length
        val joined = texts.joinToString("")

        val matches = collectMatches(joined)
        if (matches.isEmpty()) return seq

        fun styleAt(pos: Int): Style {
            val seg = segmentOffsets.indexOfLast { it <= pos }.coerceAtLeast(0)
            return styles[minOf(seg, styles.lastIndex)]
        }

        fun appendSpan(result: MutableComponent, from: Int, to: Int) {
            if (from >= to) return
            var pos = from

            while (pos < to) {
                val seg = segmentOffsets.indexOfLast { it <= pos }.coerceAtLeast(0)
                val segEnd = minOf(segmentOffsets[minOf(seg + 1, segmentOffsets.lastIndex)], to)
                val style = styles[minOf(seg, styles.lastIndex)]

                result.append(Component.literal(joined.substring(pos, segEnd)).withStyle(style))
                pos = segEnd
            }
        }

        val rebuilt = Component.empty()
        var cursor = 0
        for (match in matches) {
            appendSpan(rebuilt, cursor, match.startIndex)

            val replacementComp = match.replacement.component.copy()
            if (replacementComp.style.isEmpty) replacementComp.withStyle(styleAt(match.startIndex))

            rebuilt.append(replacementComp)
            cursor = match.startIndex + match.replacement.target.length
        }

        appendSpan(rebuilt, cursor, joined.length);

        return rebuilt.visualOrderText
    }

    private fun replaceText(text: String): String? {
        val accepted = collectMatches(text)
        if (accepted.isEmpty()) return null

        val result = StringBuilder(text.length)
        var cursor = 0

        for (match in accepted) {
            if (match.startIndex > cursor) result.append(text, cursor, match.startIndex)
            result.append(match.replacement.plainString)
            cursor = match.startIndex + match.replacement.target.length
        }

        if (cursor < text.length) result.append(text, cursor, text.length)

        return result.toString()
    }

    private fun collectMatches(text: String): List<Match> {
        val hits = mutableListOf<Match>()
        var node = ahoCorasick.root

        for (i in text.indices) {
            node = ahoCorasick.goto(node, text[i])
            var outputNode = if (node.output != null) node else node.outputLink

            while (outputNode != null) {
                val r = outputNode.output !!
                if (i + 1 == text.length || text[i + 1] == ' ')
                    hits.add(Match(i - r.target.length + 1, r))
                outputNode = outputNode.outputLink
            }
        }

        hits.sortWith(compareBy<Match> { it.startIndex }.thenByDescending { it.replacement.target.length })
        val matches = mutableListOf<Match>()
        var blockedUntil = 0
        for (hit in hits) {
            if (hit.startIndex < blockedUntil) continue
            matches.add(hit)
            blockedUntil = hit.startIndex + hit.replacement.target.length
        }
        return matches
    }

    private fun getCache(): List<Replacement> {
        val version = replacementMap.version
        if (replacementVersion == version) return preparedReplacements

        val replacements = replacementMap.entries.asSequence().map { (target, replacement) ->
            val component = parseJsonToComponent(replacement) ?: Component.literal(applyColorCodes(replacement))
            Replacement(target, component, applyColorCodes(component.string))
        }.sortedWith(compareByDescending<Replacement> { it.target.length }.thenBy { it.target }).toList()

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
            val replacement = StringBuilder("§x")
            for (char in hex) replacement.append("§").append(char)
            matcher.appendReplacement(buffer, replacement.toString())
        }
        matcher.appendTail(buffer)
        return buffer.toString().addColor()
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