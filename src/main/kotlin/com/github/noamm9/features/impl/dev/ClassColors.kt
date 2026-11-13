package com.github.noamm9.features.impl.dev

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.toColor
import net.minecraft.ChatFormatting

object ClassColors: Feature("Allows setting custom color for every dungeon class", toggled = true) {
    val archColor by ColorSetting("Archer Code", ChatFormatting.DARK_RED.toColor(), false).section("Colors")
    val bersColor by ColorSetting("Berserk Code", ChatFormatting.GOLD.toColor(), false)
    val healColor by ColorSetting("Healer Code", ChatFormatting.DARK_PURPLE.toColor(), false)
    val mageColor by ColorSetting("Mage Code", ChatFormatting.DARK_AQUA.toColor(), false)
    val tankColor by ColorSetting("Tank Code", ChatFormatting.DARK_GREEN.toColor(), false)
    val emptyColor = ColorSetting("Empty Code", ChatFormatting.GRAY.toColor(), false)

    private val reset by ButtonSetting("Reset Colors") {
        configSettings.forEach(ConfigHolder<*>::reset)
    }

    override fun toggle() = Unit
}