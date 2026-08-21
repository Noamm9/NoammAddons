package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

class TextInputSetting(name: String, defaultValue: String): ConfigHolder<String>(name, defaultValue), Savable {
    override fun write() = JsonPrimitive(value)
    override fun read(element: JsonElement) {
        value = element.asString
    }
}