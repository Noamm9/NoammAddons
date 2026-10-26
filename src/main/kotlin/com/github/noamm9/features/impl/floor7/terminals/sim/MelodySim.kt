package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.impl.MelodyTerminal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object MelodySim: SimTerminal(MelodyTerminal) {
    private const val MOVE_INTERVAL = 10
    private const val BUTTON_COLUMN = 7
    private val columns = 1 .. 5
    private val rows = 1 .. 3

    private var row = rows.first
    private var correct = 1
    private var current = 1
    private var direction = 1
    private var ticks = 0

    override fun setup(): String {
        row = rows.first
        correct = columns.random()
        current = columns.first
        direction = 1
        ticks = 0
        (0 .. 4).forEach(::createRow)
        return "Click the button on time!"
    }

    override fun tick(): List<Int> {
        if (isSolved() || ++ ticks < MOVE_INTERVAL) return emptyList()

        ticks = 0
        current += direction
        if (current == columns.first || current == columns.last) direction = - direction
        return createRow(row)
    }

    override fun click(click: TerminalClick): List<Int> {
        if (click.slotId != row * 9 + BUTTON_COLUMN) return emptyList()
        if (current != correct) {
            ticks = - MOVE_INTERVAL
            return emptyList()
        }

        correct = columns.random()
        row ++
        return listOf(0, row - 1, row, 4).distinct().flatMap(::createRow)
    }

    override fun isSolved() = row > rows.last

    private fun createRow(r: Int) = (0 .. 8).flatMap { col -> set(r * 9 + col, ItemStack(itemAt(r, col))) }

    private fun itemAt(r: Int, col: Int) = when {
        r == 0 || r == 4 -> if (col == correct) Items.MAGENTA_STAINED_GLASS_PANE else Items.BLACK_STAINED_GLASS_PANE
        col == BUTTON_COLUMN -> if (r == row) Items.LIME_TERRACOTTA else Items.RED_TERRACOTTA
        col !in columns -> Items.BLACK_STAINED_GLASS_PANE
        r != row -> Items.WHITE_STAINED_GLASS_PANE
        col == current -> Items.LIME_STAINED_GLASS_PANE
        else -> Items.RED_STAINED_GLASS_PANE
    }
}