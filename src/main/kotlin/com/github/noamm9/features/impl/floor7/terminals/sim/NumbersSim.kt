package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.impl.NumberTerminal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object NumbersSim: SimTerminal(NumberTerminal) {
    private var next = 1

    override fun setup(): String {
        next = 1
        gridSlots.zip((1 .. gridSlots.size).shuffled()).forEach { (slot, count) ->
            items[slot] = ItemStack(Items.RED_STAINED_GLASS_PANE, count)
        }
        return "Click in order!"
    }

    override fun click(click: TerminalClick): List<Int> {
        val stack = items[click.slotId] ?: return emptyList()
        if (stack.item != Items.RED_STAINED_GLASS_PANE || stack.count != next) return emptyList()

        next ++
        return set(click.slotId, ItemStack(Items.LIME_STAINED_GLASS_PANE, stack.count))
    }

    override fun isSolved() = next > gridSlots.size
}