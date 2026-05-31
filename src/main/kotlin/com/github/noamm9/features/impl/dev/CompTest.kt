package com.github.noamm9.features.impl.dev

import com.github.noamm9.features.Feature
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.features.annotations.Dev
import com.github.noamm9.ui.clickgui.components.impl.*
import com.github.noamm9.utils.render.Render2D.height
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW
import java.awt.Color

@Dev
@Suppress("unused")
object CompTest: Feature(
    "A comprehensive test feature used to verify every UI component, animation, and tooltip logic."
) {
    val customSound by SoundSetting("Click Sound", SoundEvents.UI_BUTTON_CLICK)

    // 1. Basic Toggles
    val flight by ToggleSetting("Flight Toggle", false)
        .withDescription("Enables the ability to fly around the world. Use the Flight Mode setting to change physics.")

    val autoSprint by ToggleSetting("Auto Sprint", true)
        .withDescription("Automatically holds the sprint key for you whenever you move forward.")

    // 2. Spacing Elements
    val sep1 by SeparatorSetting()
    val cat1 by CategorySetting("Numeric Controls")

    // 3. Sliders
    val speed by SliderSetting("Movement Speed", 1.0, 0.1, 5.0, 0.1)
        .withDescription("Multiplies your movement speed. Higher values may trigger anti-cheat flags on some servers.")

    val reach by SliderSetting("Reach Distance", 3.0, 1.0, 6.0, 0.5)
        .withDescription("Extends the maximum distance from which you can hit entities or interact with blocks.")

    val thickness by SliderSetting("Line Thickness", 2.0, 1.0, 10.0, 1.0)
        .withDescription("Adjusts the visual width of ESP boxes and tracer lines.")

    // 4. Dropdown
    val mode by DropdownSetting("Flight Mode", 0, listOf("Vanilla", "Motion", "Creative", "Hypixel", "Old-AAC"))
        .withDescription("Changes the bypass logic for flight. 'Vanilla' is safest for singleplayer, 'Motion' is better for servers.")

    // 5. Multi-Checkbox
    val targets by MultiCheckboxSetting("Target Entities", mutableMapOf(
        "Players" to true,
        "Zombies" to true,
        "Skeletons" to false,
        "Villagers" to false,
        "Animals" to false
    )).withDescription("Select which types of entities the combat and visual modules should focus on.")

    val sep2 by SeparatorSetting()
    val cat2 by CategorySetting("Input & Style")

    // 6. Color Pickers
    val espColor by ColorSetting("ESP Color", Color(85, 255, 255))
        .withDescription("The primary color used for all ESP highlighting and ClickGUI accents.")

    val secondaryColor by ColorSetting("Secondary", Color.MAGENTA)
        .withDescription("A secondary color used for gradients and specialized UI elements.")

    // 7. Keybind
    val panicBind by KeybindSetting("Panic Key", GLFW.GLFW_KEY_P)
        .withDescription("Pressing this key will instantly disable every active module in the mod.")

    // 8. Text Input
    val customName by TextInputSetting("Custom Tag", "Player123")
        .withDescription("The custom name displayed above your head or used in chat-based modules.")

    // 9. Button
    val resetButton by ButtonSetting("copy feature list") {
        mc.keyboardHandler.clipboard = FeatureManager.createFeatureList()
    }.withDescription("Reverts all settings in the Component Test feature back to their original factory defaults.")

    val testHud = hudElement("testHud") { context, _ ->
        val str = "testHud: FPS=${mc.fps}"
        context.text(mc.font, str, 0, 0, Color.white.rgb, true)
        return@hudElement str.width().toFloat() to str.height().toFloat()
    }

    override fun onEnable() {
        mc.player?.sendSystemMessage(Component.literal("§6[Debug] §fTest Feature Enabled"))
    }
}