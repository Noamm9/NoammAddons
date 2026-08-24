package com.github.noamm9.features.impl.dev

import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.KeybindSetting
import com.github.noamm9.event.impl.KeyboardEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.hud.HudEditorScreen
import com.github.noamm9.utils.GuiUtils
import gg.essential.universal.UMinecraft
import org.lwjgl.glfw.GLFW
import java.awt.Color

object ClickGui: Feature("A feature used to change the ClickGui configuration.", toggled = true) {
    private val openKeybind by KeybindSetting("Open Keybind")
    val accentColor by ColorSetting("Accent Color", Color(99, 176, 217), false).withDescription("The accent color used by the whole ClickGui.")
    val panelSorting by DropdownSetting("Sorting", 1, listOf("A-Z Sorting", "Width Sorting", "No Sorting")).withDescription("The order of the features in the panels.")

    val editGuiButton by ButtonSetting("Open HUD Editor") {
        GuiUtils.setScreen(HudEditorScreen())
    }.withDescription("Opens the HUD Editor Screen where you can change you HUD elements size and position.")

    val resetButton by ButtonSetting("Reset Settings") {
        accentColor.reset()
    }.withDescription("Reverts settings back to their original values.")

    override fun init() {
        register<KeyboardEvent.KeyPressed> {
            if (UMinecraft.currentScreenObj != null || event.action != GLFW.GLFW_PRESS) return@register
            if (! openKeybind.matches(event.keyEvent.key, mouse = false)) return@register

            GuiUtils.setScreen(ClickGuiScreen())
            event.isCanceled = true
        }

        register<MouseClickEvent> {
            if (UMinecraft.currentScreenObj != null || event.action != GLFW.GLFW_PRESS) return@register
            if (! openKeybind.matches(event.button, mouse = true)) return@register

            GuiUtils.setScreen(ClickGuiScreen())
            event.isCanceled = true
        }
    }

    override fun toggle() = Unit
}