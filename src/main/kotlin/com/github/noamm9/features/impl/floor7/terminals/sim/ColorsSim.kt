package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.impl.ColorsTerminal
import com.github.noamm9.utils.items.ItemUtils.hasGlint
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack

object ColorsSim: SimTerminal(ColorsTerminal) {
    private val itemTypes = listOf("wool", "stained_glass", "dye", "concrete")

    private val slotColors = mutableMapOf<Int, DyeColor>()
    private var target = DyeColor.WHITE

    override fun setup(): String {
        slotColors.clear()
        target = DyeColor.entries.random()

        val first = gridSlots.random()
        for (slot in gridSlots) {
            if (slot != first && Math.random() <= 0.65) continue

            slotColors[slot] = target
            items[slot] = ItemStack(BuiltInRegistries.ITEM.getValue(
                Identifier.withDefaultNamespace("${target.serializedName}_${itemTypes.random()}")
            ))
        }

        val colorName = if (target == DyeColor.LIGHT_GRAY) "SILVER" else target.name.replace('_', ' ')
        return "Select all the $colorName items!"
    }

    override fun click(click: TerminalClick): List<Int> {
        val stack = items[click.slotId] ?: return emptyList()
        if (slotColors[click.slotId] != target || stack.hasGlint()) return emptyList()
        return set(click.slotId, stack.copy().apply { set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true) })
    }

    override fun isSolved() = slotColors.none { (slot, color) -> color == target && items[slot]?.hasGlint() != true }
}