package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.types.ActionConfig
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
object Animations: Feature("Allows you to modify your hand view-model") {
    @JvmStatic val mainHandItemScale by NumberConfig("Item Scale", 0.0, - 1.5f, 1.5f, 0.05f).withDescription("0 is normal size. -0.5 is half size. 1 is double size.")

    @JvmStatic val mainHandX by NumberConfig("X", .0, - 2.0, 2.0, 0.01)
    @JvmStatic val mainHandY by NumberConfig("Y", 0.0, - 2.0, 2.0, 0.01)
    @JvmStatic val mainHandZ by NumberConfig("Z", 0.0, - 2.0, 2.0, 0.01)

    @JvmStatic val mainHandPositiveX by NumberConfig("Rotation X", 0f, - 50f, 50f, 1)
    @JvmStatic val mainHandPositiveY by NumberConfig("Rotation Y", 0f, - 50f, 50f, 1)
    @JvmStatic val mainHandPositiveZ by NumberConfig("Rotation Z", 0f, - 50f, 50f, 1)

    @JvmStatic val swingX by NumberConfig("swingX", 1.0, 0.0, 2.0, 0.01)
    @JvmStatic val swingY by NumberConfig("swingY", 1.0, 0.0, 2.0, 0.01)
    @JvmStatic val swingZ by NumberConfig("swingZ", 1.0, 0.0, 2.0, 0.01)

    @JvmStatic val disableHandMove by BooleanConfig("Disable hand movement").withDescription("Stops the held item from moving when you look around.")
    @JvmStatic val disableEquip by BooleanConfig("Disable equip animation").withDescription("Disables the equip animation when your held item changes.")
    @JvmStatic val disableSwingAnimation by BooleanConfig("Disable swing animation").withDescription("Disables the held item swing animation.")
    @JvmStatic val terminatorOnly by BooleanConfig("Terminator Only").withDescription("Disables the swing animation only for terminator.").showIf { disableSwingAnimation.value }

    @JvmStatic val swingSpeed by NumberConfig("Swing Speed", .0, - 2f, 1f, 0.05).hideIf { disableSwingAnimation.value && ! terminatorOnly.value }
    @JvmStatic val ignoreHaste by BooleanConfig("Ignore Haste").withDescription("Ignores the haste speed boost.").hideIf { disableSwingAnimation.value && ! terminatorOnly.value }

    private val reset by ActionConfig("Reset") {
        configSettings.forEach(ConfigHolder<*>::reset)
    }
}

