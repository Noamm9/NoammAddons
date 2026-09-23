package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils.gsonObject
import com.google.gson.JsonElement

class MultiCheckboxSetting(name: String, defaultValue: MutableMap<String, Boolean>): ConfigHolder<MutableMap<String, Boolean>>(name, defaultValue.toMutableMap()), Savable {
    override fun write() = gsonObject { value.forEach(::addProperty) }
    override fun read(element: JsonElement) = element.asJsonObject.entrySet().forEach { (k, v) -> value[k] = v.asBoolean }

    operator fun get(key: String) = value[key] ?: error("[MultiCheckboxSetting] $key not found")
}