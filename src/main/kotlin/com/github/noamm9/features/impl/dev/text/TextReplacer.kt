package com.github.noamm9.features.impl.dev.text

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.utils.catch
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization

object TextReplacer: AhoCorasick() {
    @JvmField var drawingTooltip = false

    fun init(names: Map<String, String>) {
        for ((k, v) in names) {
            val parced = parse(v) ?: continue
            put(k, parced.string, parced, parced.visualOrderText)
        }

        val mod = parse("""{"text":"","extra":[{"text":"N","color":"#4498DB","bold":true,"shadow_color":[0.17,0.36,0.52,1]},{"text":"o","color":"#588CD2","bold":true,"shadow_color":[0.23,0.34,0.51,1]},{"text":"a","color":"#6287CE","bold":true,"shadow_color":[0.26,0.33,0.51,1]},{"text":"m","color":"#6C81CA","bold":true,"shadow_color":[0.29,0.32,0.5,1]},{"text":"m","color":"#8075C2","bold":true,"shadow_color":[0.35,0.3,0.49,1]},{"text":"A","color":"#9469B9","bold":true,"shadow_color":[0.41,0.27,0.48,1]},{"text":"d","color":"#A85EC0","bold":true,"shadow_color":[0.47,0.26,0.47,1]},{"text":"d","color":"#B258B4","bold":true,"shadow_color":[0.5,0.25,0.46,1]},{"text":"o","color":"#BC52A7","bold":true,"shadow_color":[0.53,0.24,0.45,1]},{"text":"n","color":"#D0469E","bold":true,"shadow_color":[0.59,0.21,0.44,1]},{"text":"s","color":"#E43A96","bold":true,"shadow_color":[0.65,0.18,0.43,1]}]}""") !!
        put(MOD_NAME, mod.string, mod, mod.visualOrderText)

        build()
    }

    private fun parse(json: String): Component? {
        if (! json.trimStart().startsWith("{") && ! json.trimStart().startsWith("[")) return null

        return catch {
            val jsonElement = JsonParser.parseString(json)
            val result = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement)
            result.result().orElse(null)
        }
    }
}