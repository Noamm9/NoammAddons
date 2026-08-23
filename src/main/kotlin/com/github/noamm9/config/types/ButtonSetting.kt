package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder

class ButtonSetting(name: String, val playSound: Boolean = true, val action: () -> Unit): ConfigHolder<Unit>(name, Unit)