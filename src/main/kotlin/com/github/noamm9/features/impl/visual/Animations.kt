package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting

object Animations: Feature("Allows you to modify your hand view-model") {
    val mainHandItemScale by SliderSetting("Item Scale", 0.0, - 1.5f, 1.5f, 0.05f)
        .withDescription("0 is normal size. -0.5 is half size. 1 is double size.")

    val mainHandX by SliderSetting("X", .0, - 2.0, 2.0, 0.01)
    val mainHandY by SliderSetting("Y", 0.0, - 2.0, 2.0, 0.01)
    val mainHandZ by SliderSetting("Z", 0.0, - 2.0, 2.0, 0.01)

    val mainHandPositiveX by SliderSetting("Rotation X", 0f, - 50f, 50f, 1)
    val mainHandPositiveY by SliderSetting("Rotation Y", 0f, - 50f, 50f, 1)
    val mainHandPositiveZ by SliderSetting("Rotation Z", 0f, - 50f, 50f, 1)

    val swingX by SliderSetting("swingX", 1.0, 0.0, 2.0, 0.01)
    val swingY by SliderSetting("swingY", 1.0, 0.0, 2.0, 0.01)
    val swingZ by SliderSetting("swingZ", 1.0, 0.0, 2.0, 0.01)

    val disableEquip by ToggleSetting("Disable equip animation").withDescription("Disables the equip animation when your held item changes.")
    val disableSwingAnimation by ToggleSetting("Disable swing animation").withDescription("Disables the held item swing animation.")
    val terminatorOnly by ToggleSetting("Disable Terminator Only").withDescription("Disables the swing animation only for terminator.").showIf { disableSwingAnimation.value }

    val swingSpeed by SliderSetting("Swing Speed", .0, - 2f, 1f, 0.05).hideIf { disableSwingAnimation.value && !terminatorOnly.value }
    val ignoreHaste by ToggleSetting("Ignore Haste").withDescription("Ignores the haste speed boost.").hideIf { disableSwingAnimation.value && !terminatorOnly.value }

    private val reset by ButtonSetting("Reset") {
        configSettings.forEach(Setting<*>::reset)
    }
}

