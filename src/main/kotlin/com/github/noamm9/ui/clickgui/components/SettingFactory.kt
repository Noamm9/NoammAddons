package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.types.*
import com.github.noamm9.ui.clickgui.components.impl.*

object SettingFactory {
    fun toSetting(configHolder: ConfigHolder<*>) = when (configHolder) {
        is BooleanConfig -> ToggleSetting(configHolder)
        is ChoiceConfig -> DropdownSetting(configHolder)
        is MultiChoiceConfig -> MultiCheckboxSetting(configHolder)
        is ColorConfig -> ColorSetting(configHolder)
        is ColorCodeConfig -> ColorCodeSetting(configHolder)
        is KeybindConfig -> KeybindSetting(configHolder)
        is StringConfig -> TextInputSetting(configHolder)
        is SoundConfig -> SoundSetting(configHolder)
        is ActionConfig -> ButtonSetting(configHolder)
        is NumberConfig<*> -> @Suppress("UNCHECKED_CAST") SliderSetting(configHolder as NumberConfig<Number>)
        is VoidConfig -> null
        else -> error("Unknown config holder type: ${this::class.qualifiedName}")
    }
}