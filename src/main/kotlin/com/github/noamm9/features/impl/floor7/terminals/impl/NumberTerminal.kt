package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object NumberTerminal: Terminal() {
    override val enabled = ToggleSetting("Numbers", true)
    override val titleRegex = Regex("^Click in order!$")
    override val displayName = "Numbers"
    override val gridSize = 5 to 2
    override val slotCount = 36

    private val showNumbers by ToggleSetting("Numbers: Show Numbers").section("Numbers").showIf { enabled.value }
    private val firstColor by ColorSetting("Numbers: 1st Click", Color(0, 255, 0, 130)).showIf { enabled.value }
    private val secondColor by ColorSetting("Numbers: 2nd Click", Color(255, 255, 120, 130)).showIf { enabled.value }
    private val thirdColor by ColorSetting("Numbers: 3rd Click", Color(200, 0, 0, 130)).showIf { enabled.value }

    private val slotCounts = mutableMapOf<Int, Int>()

    override fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        items.filter { it.value.item == Items.RED_STAINED_GLASS_PANE }
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
            0 -> firstColor.value
            1 -> secondColor.value
            else -> thirdColor.value
        }

        TerminalSolver.drawSlot(ctx, x, y, color)

        if (showNumbers.value) {
            val count = slotCounts[click.slotId] ?: 0
            TerminalSolver.drawCenteredText(ctx, count.toString(), x, y)
        }
    }

    override fun reset() {
        super.reset()
        slotCounts.clear()
    }
}
