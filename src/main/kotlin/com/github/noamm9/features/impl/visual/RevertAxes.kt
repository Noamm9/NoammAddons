package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * @see com.github.noamm9.mixin.MixinItemModelResolver
 */
object RevertAxes: Feature("Turns certain swords back into an axe") {
    private val replaceableItems = hashMapOf(
        Pair("RAGNAROCK_AXE", Items.GOLDEN_AXE),
        Pair("DAEDALUS_AXE", Items.GOLDEN_AXE),
        Pair("STARRED_DAEDALUS_AXE", Items.GOLDEN_AXE),
        Pair("AXE_OF_THE_SHREDDED", Items.DIAMOND_AXE)
    )

    @JvmStatic
    fun itemModelHook(stack: ItemStack, key: DataComponentType<*>, original: Operation<Identifier>): Identifier {
        val currentModel = original.call(stack, key)
        if (! enabled) return currentModel
        if (stack.isEmpty) return currentModel
        val skyblockID = stack.skyblockId

        if (skyblockID !in replaceableItems.keys) return currentModel
        val replace = replaceableItems[skyblockID] ?: return currentModel
        return replace.components().get(DataComponents.ITEM_MODEL) !!
    }
}