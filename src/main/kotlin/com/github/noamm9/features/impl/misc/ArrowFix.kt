package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * @see com.github.noamm9.mixin.MixinPlayer
 */
object ArrowFix: Feature("Disables Bow Pullback on Shortbows.") {
    private val notBowCache = HashSet<String>()
    private val bowCache = HashSet<String>()

    @JvmStatic
    fun isShortbow(item: ItemStack?): Boolean {
        if (item == null || item.isEmpty) return false
        if (! item.`is`(Items.BOW)) return false
        val id = item.skyblockId
        if (id in bowCache) return true
        if (id in notBowCache) return false
        if (! item.has(DataComponents.LORE)) return false
        val lore = item.get(DataComponents.LORE)?.lines() ?: return false

        for (i in (lore.size - 3) downTo 0) {
            val line = lore[i].string
            if ("Shortbow: Instantly shoots!" in line) {
                bowCache.add(id)
                return true
            }
        }

        notBowCache.add(id)
        return false
    }
}