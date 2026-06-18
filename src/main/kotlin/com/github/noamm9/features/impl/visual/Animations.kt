package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.items.ItemUtils.skyblockId

object Animations: Feature("Allows you to modify your hand view-model") {
    val mainHandItemScale by SliderSetting("Item Scale", 1f, 0.1f, 3f, 0.05f)

    val mainHandX by SliderSetting("X", 0f, - 2.5f, 1.5f, 0.05f)
    val mainHandY by SliderSetting("Y", 0f, - 1.5f, 1.5f, 0.05f)
    val mainHandZ by SliderSetting("Z", 0f, - 1.5f, 3f, 0.05f)

    val mainHandPositiveX by SliderSetting("Rotation X", 0f, - 180f, 180f, 1f)
    val mainHandPositiveY by SliderSetting("Rotation Y", 0f, - 180f, 180f, 1f)
    val mainHandPositiveZ by SliderSetting("Rotation Z", 0f, - 180f, 180f, 1f)

    val ignoreHaste by ToggleSetting("Ignore Haste").withDescription("Makes the chosen speed override haste modifiers.")
    val swingSpeed by SliderSetting("Swing Speed", 6, 0, 32, 1).withDescription("Speed of the swing animation.").showIf { ignoreHaste.value }

    val reSwing by ToggleSetting("Re-Swing").withDescription("Lets you swing again while the swing animation is still playing.")
    val disableEquip by ToggleSetting("Disable equip animation").withDescription("Disables the equipping animation when switching items.")
    val disableSwingAnimation by ToggleSetting("Disable swing animation").withDescription("Prevents your item from visually swinging forward.")
    val noTermSwing by ToggleSetting("No Term Swing").withDescription("Stops the Terminator's swing animation, like Swing Speed 0.")

    private val reset by ButtonSetting("Reset") {
        configSettings.forEach(Setting<*>::reset)
    }

    fun shouldDisableSwing(): Boolean = enabled && disableSwingAnimation.value

    fun isTerminatorHeld(): Boolean = mc.player?.mainHandItem?.skyblockId == "TERMINATOR"
}
