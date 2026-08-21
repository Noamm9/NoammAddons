package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.location.LocationUtils

/**
 * @see com.github.noamm9.mixin.MixinAbstractRecipeBookScreen
 * @see com.github.noamm9.mixin.MixinRecipeBookComponent
 * @see com.github.noamm9.mixin.MixinGuiGraphicsExtractor
 */
object Tweaks: Feature("Small quality of life tweaks.") {
    @JvmStatic val hideRecipeBook by ToggleSetting("Hide Recipe Book").withDescription("Hides the recipe book button in inventory GUIs.")
    @JvmStatic val closeRecipeBook by ToggleSetting("Close Recipe Book").withDescription("Also closes the recipe book screen.").showIf { hideRecipeBook.value }
    @JvmStatic val hideItemCooldowns by ToggleSetting("Hide Item Cooldowns").withDescription("Hides the hotbar cooldown overlay for items.")
    @JvmStatic val hideHotbarTooltips by ToggleSetting("Hide Hotbar Tooltips").withDescription("Hides the item tooltip when switching items in hotbar.")

    @JvmStatic fun shouldHideItemCooldownOverlay() = enabled && hideItemCooldowns.value && LocationUtils.inSkyblock
}