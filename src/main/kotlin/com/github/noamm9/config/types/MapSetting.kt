package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils
import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MapSetting<K, V>(name: String, defaultValue: Map<K, V>, private val type: Type): ConfigHolder<Map<K, V>>(name, defaultValue), Savable {
    override fun write(): JsonElement = GsonUtils.gson.toJsonTree(value, type)
    override fun read(element: JsonElement) {
        value = GsonUtils.gson.fromJson(element, type)
    }

    companion object {
        inline operator fun <reified K: Any, reified V: Any> invoke(name: String, defaultValue: Map<K, V> = emptyMap()) =
            MapSetting<K, V>(name, defaultValue, object: TypeToken<Map<K, V>>() {}.type)
    }
}
