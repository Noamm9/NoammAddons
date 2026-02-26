package com.github.noamm9.features.impl.dungeon.solvers.terminals

import com.github.noamm9.NoammAddons.mc
import kotlin.math.pow
import kotlin.math.sqrt

object HumanClickOrder {
    private const val NEIGHBOR_RADIUS = 20.0

    var lastClickedSlot: Int? = null

    fun getBestClick(availableClicks: List<TerminalClick>, type: TerminalType): TerminalClick {
        if (availableClicks.isEmpty()) throw IllegalStateException("Solution list is empty")

        val middle = type.slotCount.div(2)
        if (this.lastClickedSlot == null) this.lastClickedSlot = middle

        val lastSlot = this.lastClickedSlot ?: throw IllegalStateException("Last clicked slot is null")

        val bestClick = availableClicks.shuffled().sortedWith(Comparator { clickA, clickB ->
            val distA = getDistance(clickA.slotId, lastSlot)
            val distB = getDistance(clickB.slotId, lastSlot)

            if (distA == distB) {
                val neighborsA = countNeighbors(clickA.slotId, availableClicks)
                val neighborsB = countNeighbors(clickB.slotId, availableClicks)
                return@Comparator neighborsA.compareTo(neighborsB)
            }

            return@Comparator distA.compareTo(distB)

        }).first()

        this.lastClickedSlot = bestClick.slotId

        return bestClick
    }

    fun getWorstClick(availableClicks: List<TerminalClick>, type: TerminalType): TerminalClick {
        if (availableClicks.isEmpty()) throw IllegalStateException("Solution list is empty")

        val middle = type.slotCount.div(2)
        if (this.lastClickedSlot == null) this.lastClickedSlot = middle

        val lastSlot = this.lastClickedSlot ?: throw IllegalStateException("Last clicked slot is null")

        val bestClick = availableClicks.shuffled().sortedWith(Comparator { clickA, clickB ->
            val distA = getDistance(clickA.slotId, lastSlot)
            val distB = getDistance(clickB.slotId, lastSlot)

            if (distA == distB) {
                val neighborsA = countNeighbors(clickA.slotId, availableClicks)
                val neighborsB = countNeighbors(clickB.slotId, availableClicks)
                return@Comparator neighborsA.compareTo(neighborsB)
            }

            return@Comparator distA.compareTo(distB)

        }).last()

        this.lastClickedSlot = bestClick.slotId

        return bestClick
    }

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