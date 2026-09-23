package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils
import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement

class ListSetting<V>(name: String, defaults: MutableList<V> = mutableListOf()): ConfigHolder<MutableList<V>>(name, defaults.toMutableList()), Savable {
    constructor(name: String, vararg defaults: V): this(name, defaults.toMutableList())

    private val type = object: TypeToken<MutableList<V>>() {}.type

    override fun write() = GsonUtils.gson.toJsonTree(value, type)
    override fun read(element: JsonElement) = ::value.set(GsonUtils.gson.fromJson(element, type))

    operator fun get(i: Int) = value[i]
    operator fun set(i: Int, v: V) = value.set(i, v).also { changeListener?.invoke(value) }
}