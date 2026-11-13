package com.github.noamm9.features.impl.dev

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.toColor
import net.minecraft.ChatFormatting

object ClassColors: Feature("Allows setting custom color for every dungeon class", toggled = true) {
    val archColor by ColorSetting("Archer Color", ChatFormatting.DARK_RED.toColor(), false).section("Colors").jsonName("arch")
    val bersColor by ColorSetting("Berserk Color", ChatFormatting.GOLD.toColor(), false).jsonName("bers")
    val healColor by ColorSetting("Healer Color", ChatFormatting.DARK_PURPLE.toColor(), false).jsonName("heal")
    val mageColor by ColorSetting("Mage Color", ChatFormatting.DARK_AQUA.toColor(), false).jsonName("mage")
    val tankColor by ColorSetting("Tank Color", ChatFormatting.DARK_GREEN.toColor(), false).jsonName("tank")
    val emptyColor = ColorSetting("Empty Color", ChatFormatting.GRAY.toColor(), false).jsonName("empty")

    private val reset by ButtonSetting("Reset Colors") {
        configSettings.forEach(ConfigHolder<*>::reset)
    }

    override fun toggle() = Unit
}