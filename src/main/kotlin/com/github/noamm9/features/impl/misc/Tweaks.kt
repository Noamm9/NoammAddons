package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.world.item.ItemStack

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
    @JvmStatic val noAotvPaths by ToggleSetting("No AOTV Paths").withDescription("Prevents the Aspect of the Void from creating ghost paths and playing shovel sounds.")

    @JvmStatic fun shouldHideItemCooldownOverlay() = enabled && hideItemCooldowns.value && LocationUtils.inSkyblock
    @JvmStatic fun shouldPreventAotvPaths(stack: ItemStack) = enabled && noAotvPaths.value && LocationUtils.inSkyblock && stack.skyblockId == "ASPECT_OF_THE_VOID"
}