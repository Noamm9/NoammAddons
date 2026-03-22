package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.hud.HudEditorScreen
import java.awt.Color

object ClickGui: Feature("A feature used to change the ClickGui configuration.", toggled = true) {
    val playClickSound by ToggleSetting("Click Sound", true)
        .withDescription("Toggle for the sound that plays when you click on a setting element.")

    val accsentColor by ColorSetting("Accent Color", Color.CYAN, false)
        .withDescription("The accent color used by the whole ClickGui.")

    val panelSorting by DropdownSetting("Sorting", 1, listOf("A-Z Sorting", "Width Sorting", "No Sorting"))
        .withDescription("The order of the features in the panels.")

    val editGuiButton by ButtonSetting("Open HUD Editor") {
        ClickGuiScreen.onClose()
        NoammAddons.screen = HudEditorScreen
    }.withDescription("Opens the HUD Editor Screen where you can change you HUD elements size and position.")

    val resetButton by ButtonSetting("Reset Settings") {
        playClickSound.value = true
        accsentColor.value = Color.CYAN
    }.withDescription("Reverts settings back to their original values.")

    override fun toggle() {}
}