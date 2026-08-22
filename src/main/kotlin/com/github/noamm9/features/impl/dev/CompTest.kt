package com.github.noamm9.features.impl.dev

import com.github.noamm9.config.types.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.utils.render.RenderHelper.height
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.UKeyboard
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import java.awt.Color

@Suppress("unused")
object CompTest: Feature("A test feature used to test every UI component.") {
    val customSound by SoundSetting("Click Sound", SoundEvents.UI_BUTTON_CLICK)

    val flight by ToggleSetting("test Toggle", false).withDescription("Enables the ability to fly around the world. Use the Flight Mode setting to change physics.")

    val speed by SliderSetting("test slider", 1.0, 0.1, 5.0, 0.1).withDescription("Multiplies your movement speed. Higher values may trigger anti-cheat flags on some servers.").section("test category")

    val mode by DropdownSetting("test dropdown", 0, listOf("Vanilla", "Motion", "Creative", "Hypixel", "Old-AAC")).withDescription("Changes the bypass logic for flight. 'Vanilla' is safest for singleplayer, 'Motion' is better for servers.")

    val targets by MultiCheckboxSetting("test multi checkbox", mutableMapOf(
        "Players" to true,
        "Zombies" to true,
        "Skeletons" to false,
        "Villagers" to false,
        "Animals" to false
    )).withDescription("Select which types of entities the combat and visual modules should focus on.")

    val espColor by ColorSetting("test Color", Color(85, 255, 255)).withDescription("The primary color used for all ESP highlighting and ClickGUI accents.").section("test category 2")

    val secondaryColor by ColorSetting("Secondary", Color.MAGENTA).withDescription("A secondary color used for gradients and specialized UI elements.")

    val panicBind by KeybindSetting("test Keybind", UKeyboard.KEY_P).withDescription("Pressing this key will instantly disable every active module in the mod.")

    val customName by TextInputSetting("test text", "Player123").withDescription("The custom name displayed above your head or used in chat-based modules.")

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