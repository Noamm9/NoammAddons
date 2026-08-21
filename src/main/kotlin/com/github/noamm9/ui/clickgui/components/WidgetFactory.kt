package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.types.*
import com.github.noamm9.ui.clickgui.components.impl.*

object WidgetFactory {
    fun fromSetting(configHolder: ConfigHolder<*>) = when (configHolder) {
        is ToggleSetting -> ToggleWidget(configHolder)
        is DropdownSetting -> DropdownWidget(configHolder)
        is MultiCheckboxSetting -> MultiCheckboxWidget(configHolder)
        is ColorSetting -> ColorWidget(configHolder)
        is ColorCodeSetting -> ColorCodeWidget(configHolder)
        is KeybindSetting -> KeybindWidget(configHolder)
        is TextInputSetting -> TextInputWidget(configHolder)
        is SoundSetting -> SoundWidget(configHolder)
        is ButtonSetting -> ButtonWidget(configHolder)
        is SliderSetting<*> -> @Suppress("UNCHECKED_CAST") SliderWidget(configHolder as SliderSetting<Number>)
        is UnitSetting -> null
        else -> error("Unknown config holder type: ${this::class.qualifiedName}")
    }
}