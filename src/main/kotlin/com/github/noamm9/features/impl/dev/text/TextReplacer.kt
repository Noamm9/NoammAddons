package com.github.noamm9.features.impl.dev.text

import com.github.noamm9.utils.catch
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.FormattedCharSequence

object TextReplacer: AhoCorasick() {
    private val cache = object: LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
            return size > 1000
        }
    }

    fun init(map: Map<String, String>) {
        for ((k, v) in map) {
            val comp = parse(v) ?: continue
            put(k, comp.string, comp, comp.visualOrderText)
        }

        build()
    }

    @JvmStatic fun handleString(text: String) = if (text.isBlank()) text else cache.getOrPut(text) { replaceString(text) }
    @JvmStatic fun handleComponent(component: Component) = replaceComponent(component)
    @JvmStatic fun handleCharSequence(seq: FormattedCharSequence) = replaceCharSequence(seq)

    private fun parse(json: String): MutableComponent? {
        if (! json.trimStart().startsWith("{") && ! json.trimStart().startsWith("[")) return null

        return catch {
            val jsonElement = JsonParser.parseString(json)
            val result = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement)
            result.result().orElse(null)?.copy()
        }
    }
}