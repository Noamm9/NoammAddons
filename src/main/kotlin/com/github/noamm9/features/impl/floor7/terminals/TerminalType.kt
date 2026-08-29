package com.github.noamm9.features.impl.floor7.terminals

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Items

enum class TerminalType(val slotCount: Int) {
    COLORS(54), MELODY(54), NUMBERS(36), REDGREEN(45), RUBIX(45), STARTWITH(45);

    companion object {
        val colorsRegex = Regex("^Select all the ([\\w ]+) items!$")
        val melodyRegex = Regex("^Click the button on time!$")
        val numbersRegex = Regex("^Click in order!$")
        val redgreenRegex = Regex("^Correct all the panes!$")
        val rubixRegex = Regex("^Change all to same color!$")
        val startwithRegex = Regex("^What starts with: '(\\w)'\\?$")

        val rubixOrder = listOf(
            Items.RED_STAINED_GLASS_PANE,
            Items.ORANGE_STAINED_GLASS_PANE,
            Items.YELLOW_STAINED_GLASS_PANE,
            Items.GREEN_STAINED_GLASS_PANE,
            Items.BLUE_STAINED_GLASS_PANE,
        )

        val colorReplacements = mapOf(
            Regex("^light gray") to "silver",
            Regex("^wool") to "white",
            Regex("^bone") to "white",
            Regex("^ink") to "black",
            Regex("^lapis") to "blue",
            Regex("^cocoa") to "brown",
            Regex("^dandelion") to "yellow",
            Regex("^rose") to "red",
            Regex("^cactus") to "green"
        )

        var lastRubixTarget: Int? = null

        var melodyButton: Int? = null
        var melodyCurrent: Int? = null
        var melodyCorrect: Int? = null

        val numbersSlotCounts = mutableMapOf<Int, Int>()

        val clickedSlots = mutableSetOf<Int>()
        var clickedSlot: Pair<Int, Int>? = null
        val specialItems = BuiltInRegistries.ITEM.filter {
            it.components().has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)
        } + Items.GOLDEN_APPLE

        fun fromName(windowTitle: String): TerminalType? {
            if (colorsRegex.matches(windowTitle)) return COLORS
            if (melodyRegex.matches(windowTitle)) return MELODY
            if (numbersRegex.matches(windowTitle)) return NUMBERS
            if (redgreenRegex.matches(windowTitle)) return REDGREEN
            if (rubixRegex.matches(windowTitle)) return RUBIX
            if (startwithRegex.matches(windowTitle)) return STARTWITH
            return null
        }

        fun reset() {
            lastRubixTarget = null
            melodyButton = null
            melodyCurrent = null
            melodyCorrect = null
            numbersSlotCounts.clear()
            clickedSlots.clear()
            clickedSlot = null
        }
    }
}