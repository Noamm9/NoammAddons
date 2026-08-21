package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.awt.Color

class ColorSetting(name: String, defaultValue: Color, val withAlpha: Boolean = true): ConfigHolder<Color>(name, defaultValue), Savable {
    override fun write() = JsonPrimitive(value.rgb)
    override fun read(element: JsonElement) {
        val rgb = element.asInt
        value = Color(rgb, true)
    }
}