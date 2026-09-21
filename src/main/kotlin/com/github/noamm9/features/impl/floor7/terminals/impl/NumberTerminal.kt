package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object NumberTerminal: Terminal() {
    override val titleRegex = Regex("^Click in order!$")
    override val displayName = "Numbers"
    override val gridSize = 7 to 2 // todo change back to 5x2
    override val slotCount = 36

    private val slotCounts = mutableMapOf<Int, Int>()

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        items.filter { it.value.item == Items.STAINED_GLASS_PANE.red() }
            .toList().sortedBy { it.second.count }
            .forEach { (slot, stack) ->
                slotCounts[slot] = stack.count
                solution.add(TerminalClick(slot))
            }

        return true
    }

    override fun getClickForSlot(slot: Int) = solution.firstOrNull()?.takeIf { it.slotId == slot }

    override fun renderSlot(ctx: GuiGraphicsExtractor, x: Number, y: Number, index: Int, click: TerminalClick, baseColor: Color) {
        if (index > 2) return

        val color = when (index) {
            0 -> TerminalSolver.firstColor.value
            1 -> TerminalSolver.secondColor.value
            else -> TerminalSolver.thirdColor.value
        }

        TerminalSolver.drawSlot(ctx, x, y, color)

        if (TerminalSolver.showNumbers.value) {
            val count = slotCounts[click.slotId] ?: 0
            TerminalSolver.drawCenteredText(ctx, count.toString(), x, y)
        }
    }

    override fun reset() {
        super.reset()
        slotCounts.clear()
    }
}
