package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.impl.StartWithTerminal
import com.github.noamm9.utils.items.ItemUtils.hasGlint
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object StartWithSim: SimTerminal(StartWithTerminal) {
    private val itemsByLetter by lazy {
        BuiltInRegistries.ITEM
            .filter { it != Items.AIR && it != Items.GOLDEN_APPLE && ! it.components().has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) }
            .map(::ItemStack)
            .filter { it.hoverName.string.firstOrNull()?.isLetter() == true }
            .groupBy { it.hoverName.string.first().lowercaseChar() }
            .filterValues { it.size >= 2 }
    }

    private var letter = 'a'

    override fun setup(): String {
        letter = itemsByLetter.keys.random()
        val matching = itemsByLetter.getValue(letter)

        val first = gridSlots.random()
        for (slot in gridSlots) {
            if (slot != first && Math.random() <= 0.65) continue
            items[slot] = matching.random().copy()
        }

        return "What starts with: '${letter.uppercaseChar()}'?"
    }

    override fun click(click: TerminalClick): List<Int> {
        val stack = items[click.slotId] ?: return emptyList()
        if (! isTarget(stack)) return emptyList()
        return set(click.slotId, stack.copy().apply { set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true) })
    }

    override fun isSolved() = gridSlots.none { items[it]?.let(::isTarget) == true }

    private fun isTarget(stack: ItemStack) = ! stack.hasGlint() && stack.hoverName.string.startsWith(letter, true)
}