package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.impl.RedGreenTerminal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object RedGreenSim: SimTerminal(RedGreenTerminal) {
    override fun setup(): String {
        for (slot in gridSlots) items[slot] = ItemStack(
            if (Math.random() > 0.65) Items.LIME_STAINED_GLASS_PANE
            else Items.RED_STAINED_GLASS_PANE
        )
        return "Correct all the panes!"
    }

    override fun click(click: TerminalClick): List<Int> {
        if (items[click.slotId]?.item != Items.RED_STAINED_GLASS_PANE) return emptyList()
        return set(click.slotId, ItemStack(Items.LIME_STAINED_GLASS_PANE))
    }

    override fun isSolved() = gridSlots.none { items[it]?.item == Items.RED_STAINED_GLASS_PANE }
}