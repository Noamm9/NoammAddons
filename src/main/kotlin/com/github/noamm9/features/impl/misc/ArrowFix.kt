package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * @see com.github.noamm9.mixin.MixinPlayer
 */
object ArrowFix: Feature("Disables Bow Pullback on Shortbows.") {
    private val bowCache = HashMap<String, Boolean>()

    @JvmStatic
    fun isShortbow(item: ItemStack?): Boolean {
        if (item == null || item.isEmpty || ! item.`is`(Items.BOW)) return false
        return bowCache.getOrPut(item.skyblockId) {
            item.lore.dropLast(2).any { line -> "Shortbow: Instantly shoots!" in line.removeFormatting() }
        }
    }
}