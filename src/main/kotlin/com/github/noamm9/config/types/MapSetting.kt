package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MapSetting<K, V>(name: String, defaultValue: Map<K, V>, private val type: Type): ConfigHolder<MutableMap<K, V>>(name, defaultValue.toMutableMap()), Savable {
    private val defaults = defaultValue.toMap()

    override fun write(): JsonElement = GsonUtils.gson.toJsonTree(value, type)
    override fun read(element: JsonElement) {
        value = GsonUtils.gson.fromJson(element, type)
    }

    override fun reset() {
        value = defaults.toMutableMap()
    }

    operator fun get(key: K): V? = value[key]
    operator fun set(key: K, value: V) {
        this.value[key] = value
    }
}
