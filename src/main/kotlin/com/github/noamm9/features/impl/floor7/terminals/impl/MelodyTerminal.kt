package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color
import kotlin.math.floor
import kotlin.reflect.KMutableProperty0

object MelodyTerminal: Terminal() {
    override val titleRegex = Regex("^Click the button on time!$")
    override val displayName = "Melody"
    override val gridSize = 6 to 4 // todo change back to 6x3
    override val slotCount = 54

    override val trackProgress = false

    val claySlots = listOf(16, 25, 34, 43) // todo remove 43
    var buttonRow: Int? = null
    var current: Int? = null
    var correct: Int? = null

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        if (updatedItem.item != Items.LIME_STAINED_GLASS_PANE) return true

        val newCorrect = items.entries.find { it.value.item == Items.MAGENTA_STAINED_GLASS_PANE }?.key?.minus(1)
        buttonRow = (floor(updatedSlot / 9.0) - 1).toInt()
        current = updatedSlot % 9 - 1
        if (newCorrect != null) correct = newCorrect

        return true
    }

    override fun getClickForSlot(slot: Int) = claySlots.getOrNull(slot)?.let(::TerminalClick)

    override fun maxClicks() = gridSize.second
    override fun completedClicks() = buttonRow ?: 0

    override fun progressSuffix(): String {
        val currentColumn = current ?: return ""
        val correctColumn = correct ?: return ""

        return (0 .. 4).joinToString("", " §7[", "§7]") {
            when (it) {
                currentColumn -> "§a="
                correctColumn -> "§d="
                else -> "§8="
            }
        }
    }

    override fun render(ctx: GuiGraphicsExtractor, baseColor: Color, getSlotPos: (slot: Int) -> Pair<Float, Float>, mx: Float, my: Float, hoveredSlot: KMutableProperty0<Int?>) {
        val correctColumn = correct ?: return
        val currentColumn = current ?: return
        val button = buttonRow ?: return

        val correctColumnPos = getSlotPos(correctColumn)
        val currentColumnPos = getSlotPos(button * 9 + currentColumn)
        val columnHeight = TerminalSolver.spanFor(gridSize.second)
        val slotSize = 16f

        TerminalSolver.drawSlot(ctx, correctColumnPos.first, correctColumnPos.second, TerminalSolver.columnColor.value, slotSize, columnHeight)
        TerminalSolver.drawSlot(ctx, currentColumnPos.first, currentColumnPos.second, TerminalSolver.indicatorColor.value)

        for (i in 0 until gridSize.second) {
            val (x, y) = getSlotPos(0 * 9 + gridSize.first + i * 9 - 1)
            val color = if (i == button) baseColor else TerminalSolver.wrongColor.value
            TerminalSolver.drawSlot(ctx, x, y, color)

            if (mx in x .. x + slotSize && my in y .. y + slotSize) hoveredSlot.set(i)
        }
    }

    override fun reset() {
        super.reset()
        buttonRow = null
        current = null
        correct = null
    }
}