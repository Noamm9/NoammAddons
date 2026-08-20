package com.github.noamm9.utils.dungeons.enums

import com.github.noamm9.config.types.ColorCodeConfig
import com.github.noamm9.features.impl.dev.ClassColors
import java.awt.Color

enum class DungeonClass(val setting: ColorCodeConfig) {
    Archer(ClassColors.archCode),
    Berserk(ClassColors.bersCode),
    Healer(ClassColors.healCode),
    Mage(ClassColors.mageCode),
    Tank(ClassColors.tankCode),
    Empty(ClassColors.emptyCode);

    val color get() = Color(setting.value.color !!)
    val code get() = setting.value.toString()

    companion object {
        fun fromName(name: String) = entries.find {
            it.name.equals(name, ignoreCase = true)
        } ?: Empty
    }
}