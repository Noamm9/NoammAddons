package noammaddons.features.impl

import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.*
import org.lwjgl.input.Keyboard
import java.awt.Color

@Dev
object CompTest: Feature("Testing for all of the config menu's components") {
    override fun init() = addSettings(
        ButtonSetting("Test Button") {},
        ToggleSetting("Test Toggle"),
        SliderSetting("Test Slider", 1, 100, 1, 20),
        SeperatorSetting("Test Seperator"),
        KeybindSetting("Test Keybind"),
        ColorSetting("Test Color With Alpha", Color(0, 0, 0)),
        ColorSetting("Test Color", Color(0, 0, 0), false),
        DropdownSetting("Test Dropdown", listOf("Test 1", "Test 2", "Test 3")),
        TextInputSetting("Test Input", "Test"),
        MultiCheckboxSetting("Target Entities", mapOf("Players" to false, "Monsters" to false, "Animals" to true)),

        CategorySetting("Visuals", listOf(
            ToggleSetting("Show HUD", true),
            ColorSetting("HUD Color", Color.CYAN),
            SliderSetting("HUD Opacity", 0.0, 1.0, 0.05, 1.0)
        )
        ),
        CategorySetting("Combat", listOf(
            ToggleSetting("Auto-Aim", false),
            KeybindSetting("Aim Key", Keyboard.KEY_V),
            MultiCheckboxSetting("Target Entities", mapOf("Players" to false, "Monsters" to false, "Animals" to true))
        )
        ),
    )
}