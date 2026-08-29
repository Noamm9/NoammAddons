package com.github.noamm9.config

import com.github.noamm9.NoammAddons

abstract class ConfigHolder<T>(val name: String, val defaultValue: T) {
    var jsonName = name
    var description: String? = null
    var section: String? = null
    var visibility: () -> Boolean = { true }
    var changeListener: ((T) -> Unit)? = null

    var value: T = defaultValue
        set(value) {
            field = value
            if (NoammAddons.isLoaded) {
                changeListener?.invoke(value)
            }
        }

    fun reset() = ::value.set(defaultValue)
}