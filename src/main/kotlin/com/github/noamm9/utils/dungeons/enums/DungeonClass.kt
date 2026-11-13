package com.github.noamm9.utils.dungeons.enums

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.features.impl.dev.ClassColors

enum class DungeonClass(val setting: ColorSetting) {
    Archer(ClassColors.archColor),
    Berserk(ClassColors.bersColor),
    Healer(ClassColors.healColor),
    Mage(ClassColors.mageColor),
    Tank(ClassColors.tankColor),
    Empty(ClassColors.emptyColor);

    val color get() = setting.value

    companion object {
        fun fromName(name: String) = entries.find {
            it.name.equals(name, ignoreCase = true)
        } ?: Empty
    }
}