package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.utils.items.ItemUtils.hasGlint
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object StartWithTerminal: Terminal() {
    override val titleRegex = Regex("^What starts with: '(\\w)'\\?$")
    override val displayName = "Starts With"
    override val gridSize = 7 to 3
    override val slotCount = 45

    private val specialItems by lazy {
        BuiltInRegistries.ITEM.filter {
            it.components().has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)
        } + Items.GOLDEN_APPLE
    }

    private val solved = mutableSetOf<Int>()
    private val clicked = mutableSetOf<Int>()

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        val letter = titleRegex.matchEntire(title)?.groupValues?.get(1)?.lowercase() ?: return false

        if (clicked.remove(updatedSlot)) {
            solved.add(updatedSlot)
        }

        items.forEach { (index, stack) ->
            if (! stack.hoverName.string.startsWith(letter, true)) return@forEach
            if (index in solved) return@forEach
            if ((! stack.hasGlint() || stack.item in specialItems)) {
                solution.add(TerminalClick(index))
            }
        }

        return true
    }

    override fun onSlotClick(slot: Int) {
        clicked.add(slot)
    }

    override fun reset() {
        super.reset()
        solved.clear()
        clicked.clear()
    }

    override fun getClickForSlot(slot: Int) = super.getClickForSlot(slot)?.takeUnless { it.slotId in solved }
}