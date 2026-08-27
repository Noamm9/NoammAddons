package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.utils.items.ItemUtils.hasGlint
import net.minecraft.world.item.ItemStack

object StartWithTerminal: Terminal() {
    override val titleRegex = Regex("^What starts with: '(\\w)'\\?$")
    override val displayName = "Starts With"
    override val gridSize = 7 to 3
    override val slotCount = 45

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        val letter = titleRegex.matchEntire(title)?.groupValues?.get(1) ?: return false

        items.forEach { (index, stack) ->
            if (! stack.hoverName.string.startsWith(letter, true)) return@forEach
            if (! stack.hasGlint()) solution.add(TerminalClick(index))
        }

        return true
    }
}