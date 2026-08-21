package com.github.noamm9.features.impl.dev

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.ColorCodeSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import net.minecraft.ChatFormatting

@AlwaysActive
object ClassColors: Feature("Allows setting custom color for every dungeon class", toggled = true) {
    val archCode by ColorCodeSetting("Archer Code", ChatFormatting.DARK_RED).section("Colors")
    val bersCode by ColorCodeSetting("Berserk Code", ChatFormatting.GOLD)
    val healCode by ColorCodeSetting("Healer Code", ChatFormatting.DARK_PURPLE)
    val mageCode by ColorCodeSetting("Mage Code", ChatFormatting.DARK_AQUA)
    val tankCode by ColorCodeSetting("Tank Code", ChatFormatting.DARK_GREEN)
    val emptyCode = ColorCodeSetting("Empty Code", ChatFormatting.BLACK)

    private val reset by ButtonSetting("Reset Colors") {
        configSettings.forEach(ConfigHolder<*>::reset)
    }

    override fun toggle() = Unit
}