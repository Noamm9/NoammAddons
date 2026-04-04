package com.github.noamm9.features.impl.dev.text

import com.github.noamm9.NoammAddons.mc
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap
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
    private class Snapshot(val replacements: List<Replacement>, val replacementsByFirstChar: Char2ObjectOpenHashMap<Array<Replacement>>)

    private val unchanged = Any()
    private val hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val stringCache = Collections.synchronizedMap(newBoundedCache<String, String>(2048))
    private val literalCache = Collections.synchronizedMap(newBoundedCache<Style, MutableMap<String, Any>>(64))
    private val emptySnapshot = Snapshot(emptyList(), Char2ObjectOpenHashMap())

    @Volatile
    private var replacementVersion = - 1

    @Volatile
    private var preparedSnapshot = emptySnapshot

    private val replacementMap = ReplacementMap(::clearCache)

    fun setCustomNames(replacements: Map<String, String>) {
        replacementMap.clear()
        replacementMap.putAll(replacements)
    }

    @JvmStatic
    fun replaceForRender(text: String): String {
        if (text.isEmpty()) return text

        val snapshot = getSnapshot()
        if (snapshot.replacements.isEmpty()) return text

        return synchronized(stringCache) {
            stringCache.getOrPut(text) { replaceText(text, snapshot) ?: text }
        }
    }

    @JvmStatic
    fun handleComponent(component: Component): Component {
        val snapshot = getSnapshot()
        if (snapshot.replacements.isEmpty()) return component

        return rebuildComponent(component, snapshot) ?: component
    }

    @JvmStatic
    fun handleCharSequence(seq: FormattedCharSequence): FormattedCharSequence {
        val snapshot = getSnapshot()
        if (snapshot.replacements.isEmpty()) return seq

        val styles = ArrayList<Style>()
        val texts = ArrayList<String>()
        var currentStyle = Style.EMPTY
        val currentText = StringBuilder()
        var hasMatch = false

        fun flush() {
            if (currentText.isEmpty()) return

            val text = currentText.toString()
            if (! hasMatch && containsTarget(text, snapshot)) hasMatch = true
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
            rebuilt.append(replaceLiteral(text, style, snapshot) ?: Component.literal(text).withStyle(style))
        }

        return rebuilt.visualOrderText
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

    private fun rebuildComponent(component: Component, snapshot: Snapshot): MutableComponent? {
        val replacedLiteral = (component.contents as? PlainTextContents)?.let {
            replaceLiteral(it.text(), component.style, snapshot)
        }

        var siblingsChanged = false
        val siblings = if (component.siblings.isEmpty()) null else ArrayList<Component>(component.siblings.size)

        for (sibling in component.siblings) {
            val rebuilt = rebuildComponent(sibling, snapshot)
            if (rebuilt != null) siblingsChanged = true
            siblings?.add(rebuilt ?: sibling)
        }

        if (replacedLiteral == null && ! siblingsChanged) return null

        val result = replacedLiteral ?: component.plainCopy().apply { style = component.style }
        if (result.style.isEmpty) result.style = component.style
        siblings?.forEach(result::append)
        return result
    }

    private fun replaceLiteral(text: String, parentStyle: Style, snapshot: Snapshot): MutableComponent? {
        val styleCache = synchronized(literalCache) {
            literalCache.getOrPut(parentStyle) {
                Collections.synchronizedMap(newBoundedCache<String, Any>(128))
            }
        }

        val cached = synchronized(styleCache) {
            styleCache.getOrPut(text) { buildLiteralReplacement(text, parentStyle, snapshot) ?: unchanged }
        }

        return when (cached) {
            is MutableComponent -> cached.copy()
            else -> null
        }
    }

    private fun buildLiteralReplacement(text: String, parentStyle: Style, snapshot: Snapshot): MutableComponent? {
        var index = 0
        var plainStart = 0
        var rebuilt: MutableComponent? = null

        while (index < text.length) {
            val match = matchAt(text, index, snapshot)
            if (match == null) {
                index ++
                continue
            }

            val result = rebuilt ?: Component.empty().also { rebuilt = it }
            if (index > plainStart) {
                result.append(Component.literal(text.substring(plainStart, index)).withStyle(parentStyle))
            }
            val replacementComponent = match.component.copy()
            if (replacementComponent.style.isEmpty) replacementComponent.withStyle(parentStyle)
            result.append(replacementComponent)

            index += match.target.length
            plainStart = index
        }

        val result = rebuilt ?: return null
        if (plainStart < text.length) result.append(Component.literal(text.substring(plainStart)).withStyle(parentStyle))
        if (result.style.isEmpty) result.style = parentStyle
        return result
    }

    private fun replaceText(text: String, snapshot: Snapshot): String? {
        var index = 0
        var plainStart = 0
        var rebuilt: StringBuilder? = null

        while (index < text.length) {
            val match = matchAt(text, index, snapshot)
            if (match == null) {
                index ++
                continue
            }

            val result = rebuilt ?: StringBuilder(text.length).also { rebuilt = it }
            if (index > plainStart) result.append(text, plainStart, index)
            result.append(match.plainString)
            index += match.target.length
            plainStart = index
        }

        val result = rebuilt ?: return null
        if (plainStart < text.length) result.append(text, plainStart, text.length)
        return result.toString()
    }

    private fun containsTarget(text: String, snapshot: Snapshot): Boolean {
        for (index in text.indices) {
            if (matchAt(text, index, snapshot) != null) return true
        }
        return false
    }

    private fun matchAt(text: String, index: Int, snapshot: Snapshot): Replacement? {
        val candidates = snapshot.replacementsByFirstChar.get(text[index]) ?: return null
        for (replacement in candidates) {
            if (text.regionMatches(index, replacement.target, 0, replacement.target.length, false)) {
                return replacement
            }
        }
        return null
    }

    private fun getSnapshot(): Snapshot {
        val version = replacementMap.version
        if (replacementVersion == version) return preparedSnapshot

        val replacements = replacementMap.entries.asSequence()
            .filter { (target, replacement) -> target.isNotEmpty() && replacement.isNotEmpty() }
            .map { (target, replacement) ->
                val component = parseJsonToComponent(replacement) ?: Component.literal(applyColorCodes(replacement))
                Replacement(target, component, applyColorCodes(component.string))
            }
            .sortedWith(compareByDescending<Replacement> { it.target.length }.thenBy { it.target })
            .toList()

        val mutableBuckets = Char2ObjectOpenHashMap<MutableList<Replacement>>()
        for (replacement in replacements) {
            val firstChar = replacement.target[0]
            val bucket = mutableBuckets.get(firstChar)
            if (bucket == null) mutableBuckets.put(firstChar, mutableListOf(replacement))
            else bucket.add(replacement)
        }

        val buckets = Char2ObjectOpenHashMap<Array<Replacement>>(mutableBuckets.size)
        for (entry in mutableBuckets.char2ObjectEntrySet()) {
            buckets.put(entry.charKey, entry.value.toTypedArray())
        }

        preparedSnapshot = Snapshot(replacements, buckets)
        replacementVersion = version
        return preparedSnapshot
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
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > maxEntries
        }
    }

    private fun clearCache() {
        synchronized(stringCache) { stringCache.clear() }
        synchronized(literalCache) { literalCache.clear() }
        preparedSnapshot = emptySnapshot
        replacementVersion = - 1
    }
}