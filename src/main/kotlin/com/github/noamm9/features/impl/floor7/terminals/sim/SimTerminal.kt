package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.impl.Terminal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

sealed class SimTerminal(val terminal: Terminal) {
    val items = mutableMapOf<Int, ItemStack>()
    var title = ""
        private set

    val name get() = terminal.displayName.replace(" ", "").lowercase()

    protected val gridSlots by lazy {
        val (width, height) = terminal.gridSize
        val startCol = (9 - width) / 2
        (0 until height).flatMap { row -> (0 until width).map { col -> (row + 1) * 9 + startCol + col } }
    }

    fun generate() {
        items.clear()
        for (slot in 0 until terminal.slotCount) items[slot] = ItemStack(Items.BLACK_STAINED_GLASS_PANE)
        title = setup()
    }

    protected abstract fun setup(): String
    abstract fun click(click: TerminalClick): List<Int>
    abstract fun isSolved(): Boolean
    open fun tick(): List<Int> = emptyList()

    protected fun set(slot: Int, stack: ItemStack): List<Int> {
        items[slot] = stack
        return listOf(slot)
    }

    companion object {
        val all = SimTerminal::class.sealedSubclasses.mapNotNull { it.objectInstance }

        fun fromName(name: String) = all.find { it.name == name.lowercase() }
    }
}