package com.github.noamm9.features.impl.dev.text

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.RegistryOps
import net.minecraft.util.FormattedCharSequence
import java.util.regex.*

object TextReplacer {
    private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val replaceMap = mutableMapOf<String, String>()
    private val engine = AhoCorasick()

    fun add(map: Map<String, String>) {
        replaceMap.putAll(map)

        val snapshot = HashMap(replaceMap)
        engine.build(snapshot) { raw ->
            val parsed = parseComponent(raw)
            val plainSource = parsed?.string ?: raw
            val plain = applyColorCodes(plainSource)
            val comp = parsed?.copy() ?: Component.literal(applyColorCodes(plainSource))
            plain to comp
        }
    }

    @JvmStatic
    fun handleString(text: String): String {
        if (text.isEmpty()) return text
        return engine.replaceString(text)
    }

    @JvmStatic
    fun handleComponent(component: Component): Component {
        if (engine.isEmpty()) return component
        return engine.replaceComponent(component)
    }

    @JvmStatic
    fun handleCharSequence(seq: FormattedCharSequence): FormattedCharSequence {
        if (engine.isEmpty()) return seq
        return engine.replaceCharSequence(seq)
    }

    private fun parseComponent(json: String): MutableComponent? {
        if (! json.trimStart().startsWith("{") && ! json.trimStart().startsWith("[")) return null

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

    private fun applyColorCodes(text: String): String {
        val m = HEX_PATTERN.matcher(text)
        val buffer = StringBuffer()
        while (m.find()) {
            val hex = m.group(1)
            val r = StringBuilder("§x")
            for (c in hex) r.append("§").append(c)
            m.appendReplacement(buffer, r.toString())
        }
        m.appendTail(buffer)
        return buffer.toString().replace("&", "§")
    }
}