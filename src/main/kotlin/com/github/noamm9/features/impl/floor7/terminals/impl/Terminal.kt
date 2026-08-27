package com.github.noamm9.features.impl.floor7.terminals.impl

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import java.awt.Color
import kotlin.reflect.KMutableProperty0


sealed class Terminal {
    abstract val titleRegex: Regex
    abstract val slotCount: Int
    abstract val gridSize: Pair<Int, Int>

    abstract val displayName: String
    open val trackProgress = true

    val solution = mutableListOf<TerminalClick>()
    private var totalClicks = - 1

    protected abstract fun onSlotUpdate(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean
    open fun onSlotClick(slot: Int) = Unit

    fun solve(items: Map<Int, ItemStack>, title: String, updatedSlot: Int, updatedItem: ItemStack): Boolean {
        solution.clear()
        if (! onSlotUpdate(items, title, updatedSlot, updatedItem)) return false

        if (trackProgress) refreshProgress()
        return true
    }

    open fun getClickForSlot(slot: Int) = solution.find { it.slotId == slot }

    open fun predict(click: TerminalClick) {
        solution.remove(click)
    }

    open fun reset() {
        solution.clear()
        totalClicks = - 1
    }

    open fun renderSlot(ctx: GuiGraphicsExtractor, x: Number, y: Number, index: Int, click: TerminalClick, baseColor: Color) = TerminalSolver.drawSlot(ctx, x, y, baseColor)
    open fun render(ctx: GuiGraphicsExtractor, baseColor: Color, getSlotPos: (slot: Int) -> Pair<Float, Float>, mx: Float, my: Float, hoveredSlot: KMutableProperty0<Int?>) = Unit

    protected open fun remainingClicks() = solution.size
    open fun maxClicks() = totalClicks.takeIf { it > 0 }
    open fun completedClicks() = if (totalClicks == - 1) 0 else totalClicks - remainingClicks()
    open fun progressSuffix() = ""

    protected fun refreshProgress() {
        if (totalClicks == - 1) totalClicks = remainingClicks()
    }

    companion object {
        val all = Terminal::class.sealedSubclasses.mapNotNull { it.objectInstance }

        fun fromTitle(title: String) = all.firstOrNull { it.titleRegex.matches(title) }
    }
}