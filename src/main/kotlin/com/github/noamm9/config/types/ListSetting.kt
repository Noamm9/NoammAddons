package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils
import com.google.common.reflect.TypeToken
import com.google.gson.JsonElement
import java.lang.reflect.Type

class ListSetting<V>(name: String, defaults: MutableList<V>, private val type: Type): ConfigHolder<MutableList<V>>(name, defaults.toMutableList()), Savable {
    override fun write() = GsonUtils.gson.toJsonTree(value, type)
    override fun read(element: JsonElement) = ::value.set(GsonUtils.gson.fromJson(element, type))

    operator fun get(i: Int) = value[i]
    operator fun set(i: Int, v: V) = value.set(i, v).also { changeListener?.invoke(value) }

    companion object {
        inline operator fun <reified V> invoke(name: String, defaults: MutableList<V> = mutableListOf()) = ListSetting(name, defaults.toMutableList(), object: TypeToken<MutableList<V>>() {}.type)
        inline operator fun <reified V> invoke(name: String, vararg defaults: V) = ListSetting(name, defaults.toMutableList(), object: TypeToken<MutableList<V>>() {}.type)
    }
}