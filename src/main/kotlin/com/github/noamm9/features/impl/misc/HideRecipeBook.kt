package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription

/**
 * @see com.github.noamm9.mixin.MixinAbstractRecipeBookScreen
 * @see com.github.noamm9.mixin.MixinRecipeBookComponent
 */
object HideRecipeBook: Feature("Hides the recipe book button in inventory GUIs.") {
    @JvmStatic val closeRecipeBook by ToggleSetting("Close Recipe Book", false).withDescription("Also closes the recipe book screen.")
}
