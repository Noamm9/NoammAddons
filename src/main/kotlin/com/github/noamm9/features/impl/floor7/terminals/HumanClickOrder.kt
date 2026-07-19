package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.NoammAddons.mc
import kotlin.math.pow
import kotlin.math.sqrt

object HumanClickOrder {
    private const val NEIGHBOR_RADIUS = 20.0

    var lastClickedSlot: Int? = null

    private fun selectClick(availableClicks: List<TerminalClick>, type: TerminalType, worst: Boolean): TerminalClick {
        if (availableClicks.isEmpty()) {
            throw IllegalStateException("Solution list is empty")
        }

        val lastSlot = lastClickedSlot ?: (type.slotCount / 2).also {
            lastClickedSlot = it
        }

        val comparator = Comparator<TerminalClick> { clickA, clickB ->
            val distanceComparison = getDistance(clickA.slotId, lastSlot).compareTo(getDistance(clickB.slotId, lastSlot))

            if (distanceComparison != 0) {
                distanceComparison
            } else {
                countNeighbors(clickA.slotId, availableClicks).compareTo(countNeighbors(clickB.slotId, availableClicks))
            }
        }

        val shuffled = availableClicks.shuffled()
        val selected = if (worst) { shuffled.asReversed().maxWith(comparator) } else { shuffled.minWith(comparator) }

        lastClickedSlot = selected.slotId
        return selected
    }

    fun getBestClick(availableClicks: List<TerminalClick>, type: TerminalType) = selectClick(availableClicks, type, worst = false)

    fun getWorstClick(availableClicks: List<TerminalClick>, type: TerminalType) = selectClick(availableClicks, type, worst = true)

    private fun countNeighbors(targetId: Int, allClicks: List<TerminalClick>): Int {
        return allClicks.count { other ->
            other.slotId != targetId && getDistance(targetId, other.slotId) <= NEIGHBOR_RADIUS
        }
    }

    private fun getDistance(id1: Int, id2: Int): Double {
        val pos1 = mc.player?.containerMenu?.getSlot(id1) ?: return Double.MAX_VALUE
        val pos2 = mc.player?.containerMenu?.getSlot(id2) ?: return Double.MAX_VALUE

        val dx = (pos1.x - pos2.x).toDouble()
        val dy = (pos1.y - pos2.y).toDouble()

        return sqrt(dx.pow(2) + dy.pow(2))
    }
}