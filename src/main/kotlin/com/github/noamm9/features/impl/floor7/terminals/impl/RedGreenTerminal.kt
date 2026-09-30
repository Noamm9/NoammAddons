package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object RedGreenTerminal: Terminal() {
    override val titleRegex = Regex("^Correct all the panes!$")
    override val displayName = "Red Green"
    override val gridSize = 5 to 3
    override val slotCount = 45

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        items.filter { it.value.item == Items.STAINED_GLASS_PANE.red() }
            .forEach { solution.add(TerminalClick(it.key)) }

        return true
    }
}
