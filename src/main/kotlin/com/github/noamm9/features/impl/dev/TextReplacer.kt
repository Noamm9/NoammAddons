package com.github.noamm9.features.impl.dev

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.resources.RegistryOps
import net.minecraft.util.FormattedCharSequence
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object TextReplacer {
    val replaceMap = ConcurrentHashMap<String, String>().apply {
        put("NoammAddons", "{\"text\":\"\",\"extra\":[{\"text\":\"N\",\"color\":\"#ED2AE6\",\"bold\":true},{\"text\":\"o\",\"color\":\"#DF40E5\",\"bold\":true},{\"text\":\"a\",\"color\":\"#D14FE4\",\"bold\":true},{\"text\":\"m\",\"color\":\"#C25AE3\",\"bold\":true},{\"text\":\"m\",\"color\":\"#B263E2\",\"bold\":true},{\"text\":\"A\",\"color\":\"#A26AE1\",\"bold\":true},{\"text\":\"d\",\"color\":\"#9171DF\",\"bold\":true},{\"text\":\"d\",\"color\":\"#7E76DE\",\"bold\":true},{\"text\":\"o\",\"color\":\"#687BDD\",\"bold\":true},{\"text\":\"n\",\"color\":\"#4E7FDC\",\"bold\":true},{\"text\":\"s\",\"color\":\"#2283DB\",\"bold\":true}]}")
    }

    private val cache = ConcurrentHashMap<String, Component>()

    private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")

    @JvmStatic
    fun handleString(text: String): String {
        if (text.isEmpty()) return text
        if (replaceMap.keys.none { text.contains(it) }) return text

        var result = text
        replaceMap.forEach { (target, replacement) ->
            if (! result.contains(target)) return@forEach
            val cachedComp = cache.computeIfAbsent(replacement) {
                parseJsonToComponent(it) ?: Component.literal(applyColorCodes(it))
            }

            result = result.replace(target, applyColorCodes(cachedComp.string))
        }
        return result
    }

    @JvmStatic
    fun handleComponent(component: Component): Component {
        if (replaceMap.keys.none { component.string.contains(it) }) return component
        return rebuildComponent(component)
    }

    private fun rebuildComponent(comp: Component): MutableComponent {
        val contents = comp.contents
        val newComp = if (contents is PlainTextContents) {
            val originalText = contents.text()
            val target = replaceMap.keys.find { originalText.contains(it) }

            if (target != null) injectReplacement(originalText, target, replaceMap[target] !!, comp.style)
            else (comp.copy() as MutableComponent).apply { siblings.clear() }
        }
        else (comp.copy() as MutableComponent).apply { siblings.clear() }

        if (newComp.style.isEmpty) newComp.style = comp.style

        for (sibling in comp.siblings) newComp.append(rebuildComponent(sibling))

        return newComp
    }

    private fun injectReplacement(text: String, target: String, replacement: String, parentStyle: Style): MutableComponent {
        val root = Component.literal("")
        val parts = text.split(target, limit = 2)

        if (parts[0].isNotEmpty()) root.append(Component.literal(parts[0]).withStyle(parentStyle))
        val replacementComp = cache.computeIfAbsent(replacement) {
            parseJsonToComponent(it) ?: Component.literal(applyColorCodes(it))
        }.copy()

        if (replacementComp.style.isEmpty) replacementComp.withStyle(parentStyle)
        root.append(replacementComp)

        if (parts.size > 1 && parts[1].isNotEmpty()) {
            val remaining = parts[1]
            if (remaining.contains(target)) root.append(injectReplacement(remaining, target, replacement, parentStyle))
            else root.append(Component.literal(remaining).withStyle(parentStyle))
        }
        return root
    }

    private fun parseJsonToComponent(json: String): MutableComponent? {
        if (! json.trim().startsWith("{") && ! json.trim().startsWith("[")) return null

        try {
            val jsonElement = JsonParser.parseString(json)

            val registry = Minecraft.getInstance().level?.registryAccess()
                ?: Minecraft.getInstance().connection?.registryAccess()

            val ops = if (registry != null) RegistryOps.create(JsonOps.INSTANCE, registry)
            else JsonOps.INSTANCE

            val result = ComponentSerialization.CODEC.parse(ops, jsonElement)

            return result.result().orElse(null)?.copy()
        }
        catch (_: Exception) {
            return null
        }
    }

    @JvmStatic
    fun handleCharSequence(seq: FormattedCharSequence): FormattedCharSequence {
        val sb = StringBuilder()
        seq.accept { _, _, codePoint -> sb.append(codePoint.toChar()); true }
        if (replaceMap.none { sb.toString().contains(it.key) }) return seq

        val rebuilt = Component.literal("")
        var currentStyle: Style? = null
        val buffer = StringBuilder()

        seq.accept { _, style, codePoint ->
            if (style != currentStyle) {
                if (buffer.isNotEmpty()) {
                    rebuilt.append(Component.literal(buffer.toString()).withStyle(currentStyle))
                    buffer.clear()
                }
                currentStyle = style
            }
            buffer.append(codePoint.toChar())
            true
        }
        if (buffer.isNotEmpty()) {
            rebuilt.append(Component.literal(buffer.toString()).withStyle(currentStyle))
        }

        return handleComponent(rebuilt).visualOrderText
    }

    private fun applyColorCodes(text: String): String {
        val m = HEX_PATTERN.matcher(text)
        val sb = StringBuffer()
        while (m.find()) {
            val hex = m.group(1)
            val r = StringBuilder("§x")
            for (c in hex) r.append("§").append(c)
            m.appendReplacement(sb, r.toString())
        }
        m.appendTail(sb)
        return sb.toString().replace("&", "§")
    }
}