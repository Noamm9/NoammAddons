package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils
import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement

class MapSetting<K, V>(name: String, defaults: MutableMap<K, V> = mutableMapOf()): ConfigHolder<MutableMap<K, V>>(name, defaults.toMutableMap()), Savable {
    constructor(name: String, vararg defaults: Pair<K, V>): this(name, defaults.toMap().toMutableMap())

    private val type = object: TypeToken<MutableMap<K, V>>() {}.type

    override fun write() = GsonUtils.gson.toJsonTree(value, type)
    override fun read(element: JsonElement) = ::value.set(GsonUtils.gson.fromJson(element, type))

    operator fun get(k: K) = value[k]
    operator fun set(k: K, v: V) = value.set(k, v).also { changeListener?.invoke(value) }
}