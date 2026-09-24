package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder

class ButtonSetting(name: String, val clickSound: Boolean = true, private val action: () -> Unit): ConfigHolder<Unit>(name, Unit) {
    operator fun invoke() = action.invoke()
}