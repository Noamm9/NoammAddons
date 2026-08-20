package com.github.noamm9.features.impl.dev

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.types.ActionConfig
import com.github.noamm9.config.types.ColorCodeConfig
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import net.minecraft.ChatFormatting

@AlwaysActive
object ClassColors: Feature("Allows setting custom color for every dungeon class", toggled = true) {
    val archCode by ColorCodeConfig("Archer Code", ChatFormatting.DARK_RED).section("Colors")
    val bersCode by ColorCodeConfig("Berserk Code", ChatFormatting.GOLD)
    val healCode by ColorCodeConfig("Healer Code", ChatFormatting.DARK_PURPLE)
    val mageCode by ColorCodeConfig("Mage Code", ChatFormatting.DARK_AQUA)
    val tankCode by ColorCodeConfig("Tank Code", ChatFormatting.DARK_GREEN)
    val emptyCode = ColorCodeConfig("Empty Code", ChatFormatting.BLACK)

    private val reset by ActionConfig("Reset Colors") {
        configSettings.forEach(ConfigHolder<*>::reset)
    }

    override fun toggle() = Unit
}