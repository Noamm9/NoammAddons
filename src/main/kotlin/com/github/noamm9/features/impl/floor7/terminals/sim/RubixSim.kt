package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.impl.RubixTerminal
import net.minecraft.world.item.ItemStack

object RubixSim: SimTerminal(RubixTerminal) {
    override fun setup(): String {
        do {
            gridSlots.forEach { items[it] = ItemStack(RubixTerminal.rubixOrder.random()) }
        } while (isSolved())

        return "Change all to same color!"
    }

    override fun click(click: TerminalClick): List<Int> {
        val index = RubixTerminal.rubixOrder.indexOf(items[click.slotId]?.item)
        if (index == - 1) return emptyList()

        val change = if (click.btn == 1) - 1 else 1
        return set(click.slotId, ItemStack(
            // % operator returns -1 instead of 4 for negative indices
            RubixTerminal.rubixOrder[(index + change).mod(RubixTerminal.rubixOrder.size)]
        ))
    }

    override fun isSolved() = gridSlots.map { items[it]?.item }.distinct().size == 1
}