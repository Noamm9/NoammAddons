package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils
import com.google.gson.JsonElement
import java.lang.reflect.Type

class ListSetting<V>(name: String, private val defaults: List<V> = emptyList(), private val type: Type): ConfigHolder<MutableList<V>>(name, defaults.toMutableList()), Savable {
    override fun write(): JsonElement = GsonUtils.gson.toJsonTree(value, type)
    override fun read(element: JsonElement) {
        value = GsonUtils.gson.fromJson(element, type)
    }

    override fun reset() {
        value = defaults.toMutableList()
    }

    operator fun get(index: Int): V = value[index]
    operator fun set(index: Int, value: V) {
        this.value[index] = value
    }
}
