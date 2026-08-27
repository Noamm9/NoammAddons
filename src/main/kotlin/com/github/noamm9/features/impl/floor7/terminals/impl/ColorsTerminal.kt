package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.utils.items.ItemUtils.hasGlint
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object ColorsTerminal: Terminal() {
    override val titleRegex = Regex("^Select all the ([\\w ]+) items!$")
    override val displayName = "Colors"
    override val gridSize = 7 to 4
    override val slotCount = 54

    private fun DyeColor.getValidPrefixes() = when (this) {
        DyeColor.BLACK -> setOf("black", "ink")
        DyeColor.BLUE -> setOf("blue", "lapis")
        DyeColor.BROWN -> setOf("brown", "cocoa")
        DyeColor.WHITE -> setOf("white", "bone", "wool")
        DyeColor.GREEN -> setOf("green", "cactus")
        DyeColor.RED -> setOf("red", "rose")
        DyeColor.YELLOW -> setOf("yellow", "dandelion")
        DyeColor.LIGHT_GRAY -> setOf("silver", "light gray")
        else -> setOf(name.lowercase().replace('_', ' '))
    }

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        val color = DyeColor.entries.find { it.name.replace("_", " ").equals(titleRegex.find(title)?.groupValues?.get(1)?.replace("SILVER", "LIGHT GRAY"), true) }
            ?: return false

        for ((slot, stack) in items) {
            if (stack.item == Items.BLACK_STAINED_GLASS_PANE) continue
            if (stack.hasGlint()) continue
            if (color.getValidPrefixes().any(stack.hoverName.string.lowercase()::startsWith)) {
                solution.add(TerminalClick(slot))
            }
        }

        return true
    }
}