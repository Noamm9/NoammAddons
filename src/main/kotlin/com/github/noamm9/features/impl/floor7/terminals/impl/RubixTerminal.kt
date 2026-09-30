package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.math.abs

object RubixTerminal: Terminal() {
    override val titleRegex = Regex("^Change all to same color!$")
    override val displayName = "Rubix"
    override val gridSize = 3 to 3
    override val slotCount = 45

    private val rubixOrder = listOf(
        Items.STAINED_GLASS_PANE.red(),
        Items.STAINED_GLASS_PANE.orange(),
        Items.STAINED_GLASS_PANE.yellow(),
        Items.STAINED_GLASS_PANE.green(),
        Items.STAINED_GLASS_PANE.blue(),
    )

    private val allowedSlots = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        val panes = items.filter { it.key in allowedSlots && it.value.item in rubixOrder }
        val costs = IntArray(5) { 0 }
        for (i in 0 until 5) {
            panes.forEach { (_, itemStack) ->
                val itemIdx = rubixOrder.indexOf(itemStack.item)
                if (itemIdx != - 1) {
                    val dist = abs(i - itemIdx)
                    costs[i] += if (dist > 2) 5 - dist else dist
                }
            }
        }

        val origin = costs.indices.minByOrNull { costs[it] } ?: 0
        panes.forEach { (slotId, stack) ->
            val currentIdx = rubixOrder.indexOf(stack.item)
            if (currentIdx != - 1 && currentIdx != origin) {
                var diff = origin - currentIdx
                if (diff > 2) diff -= 5
                if (diff < - 2) diff += 5
                solution.add(TerminalClick(slotId, diff))
            }
        }

        return true
    }

    override fun remainingClicks() = solution.sumOf { abs(it.btn) }

    override fun getClickForSlot(slot: Int): TerminalClick? {
        return solution.find { it.slotId == slot }?.btn?.let { TerminalClick(slot, if (it > 0) 0 else 1) }
    }

    override fun predict(click: TerminalClick) {
        val currentSolution = solution.find { it.slotId == click.slotId } ?: return
        val change = if (click.btn == 0) - 1 else 1
        val newDiff = currentSolution.btn + change

        if (newDiff == 0) solution.remove(currentSolution)
        else solution[solution.indexOf(currentSolution)] = TerminalClick(click.slotId, newDiff)
    }

    override fun renderSlot(ctx: GuiGraphicsExtractor, x: Number, y: Number, index: Int, click: TerminalClick, baseColor: Color) {
        val color = if (click.btn > 0) TerminalSolver.positiveColor.value else TerminalSolver.negativeColor.value
        TerminalSolver.drawSlot(ctx, x, y, color)
        TerminalSolver.drawCenteredText(ctx, "${click.btn}", x, y)
    }
}
