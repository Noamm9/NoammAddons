package com.github.noamm9.features.impl.dev

import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.ColorCodeSetting
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
        configSettings.forEach(Setting<*>::reset)
    }

    override fun toggle() {}
}