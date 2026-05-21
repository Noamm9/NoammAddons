package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object RevertAxes: Feature("Turns certain swords back into an axe") {
    private val replaceableItems = hashMapOf(
        Pair("RAGNAROCK_AXE", Items.GOLDEN_AXE),
        Pair("DAEDALUS_AXE", Items.GOLDEN_AXE),
        Pair("STARRED_DAEDALUS_AXE", Items.GOLDEN_AXE),
        Pair("AXE_OF_THE_SHREDDED", Items.DIAMOND_AXE)
    )

    @JvmStatic
    fun shouldReplace(original: ItemStack?): ItemStack? {
        if (! enabled) return original
        if (original == null) return original
        if (original.isEmpty) return original
        val skyblockID = original.skyblockId.takeUnless { it.isEmpty() } ?: return original
        val replace = replaceableItems[skyblockID] ?: return original
        return original.transmuteCopy(replace, original.count) ?: original
    }
}