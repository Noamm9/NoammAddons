package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils
import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MapSetting<K, V>(name: String, defaults: MutableMap<K, V>, private val type: Type): ConfigHolder<MutableMap<K, V>>(name, defaults.toMutableMap()), Savable {
    override fun write() = GsonUtils.gson.toJsonTree(value, type)
    override fun read(element: JsonElement) = ::value.set(GsonUtils.gson.fromJson(element, type) as MutableMap<K, V>)

    operator fun get(k: K) = value[k]
    operator fun set(k: K, v: V) = value.set(k, v).also { changeListener?.invoke(value) }

    companion object {
        inline operator fun <reified K, reified V> invoke(name: String, defaults: MutableMap<K, V> = mutableMapOf()) = MapSetting(name, defaults, object: TypeToken<MutableMap<K, V>>() {}.type)
        inline operator fun <reified K, reified V> invoke(name: String, vararg defaults: Pair<K, V>) = MapSetting(name, defaults.toMap().toMutableMap(), object: TypeToken<MutableMap<K, V>>() {}.type)
    }
}