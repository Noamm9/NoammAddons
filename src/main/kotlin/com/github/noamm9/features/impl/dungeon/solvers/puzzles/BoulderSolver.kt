package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.boxColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.clickColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.showAll
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.WallSignBlock

object BoulderSolver {
    private data class BoulderBox(val box: BlockPos, val click: BlockPos, val render: BlockPos)

    private val boulderSolutions by lazy { DataDownloader.loadJson<Map<String, List<List<Double>>>>("boulderSolutions.json") }
    private var currentSolution = mutableListOf<BoulderBox>()

    private var inBoulder = false
    private var roomCenter = BlockPos.ZERO
    private var rotation = 0


    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Boulder") return

        inBoulder = true
        rotation = event.room.rotation?.let { 360 - it + 180 } ?: return
        roomCenter = event.room.centerPos

        solve()
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! inBoulder || currentSolution.isEmpty()) return
        if (showAll.value) currentSolution.forEach { renderBox(ctx, it) }
        else renderBox(ctx, currentSolution.first())
    }

    fun onInteract(event: PlayerInteractEvent.RIGHT_CLICK.BLOCK) {
        if (! inBoulder) return
        when (WorldUtils.getBlockAt(event.pos)) {
            is ButtonBlock, is LeverBlock, is WallSignBlock -> {
                val entry = currentSolution.find { it.click == event.pos } ?: return
                currentSolution.remove(entry)
            }

            is ChestBlock -> reset()
        }
    }

    private fun solve() {
        val sx = - 9
        val sy = 65
        val sz = - 9

        val sb = StringBuilder()

        for (z in 0 .. 5) for (x in 0 .. 6) {
            val pos = ScanUtils.getRealCoord(BlockPos(sx + x * 3, sy, sz + z * 3), roomCenter, rotation)
            sb.append(if (WorldUtils.getStateAt(pos).isAir) "0" else "1")
        }

        val pattern = sb.toString()
        val solutions = boulderSolutions[pattern] ?: return

        currentSolution = solutions.map { sol ->
            val box = ScanUtils.getRealCoord(BlockPos(sol[0].toInt(), sy, sol[1].toInt()), roomCenter, rotation)
            val click = ScanUtils.getRealCoord(BlockPos(sol[2].toInt(), sy, sol[3].toInt()), roomCenter, rotation)
            val render = ScanUtils.getRealCoord(BlockPos(sol[4].toInt(), sy, sol[5].toInt()), roomCenter, rotation)
            BoulderBox(box, click, render)
        }.toMutableList()
    }

    private fun renderBox(ctx: RenderContext, box: BoulderBox) {
        Render3D.renderBox(
            ctx,
            box.box.x + 0.5, box.box.y - 1.0, box.box.z + 0.5,
            3.0, 3.0,
            boxColor.value,
            outline = true,
            fill = true,
            phase = true
        )

        Render3D.renderBlock(ctx, box.click, clickColor.value, phase = true)
    }

    fun reset() {
        inBoulder = false
        roomCenter = BlockPos.ZERO
        rotation = 0
        currentSolution.clear()
    }
}