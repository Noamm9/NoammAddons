package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.phase
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.renderLines
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object CreeperBeamSolver {
    private data class BeamPair(val start: BlockPos, val end: BlockPos, val color: Color = Color.WHITE)

    private val beamSolutions by lazy {
        DataDownloader.loadJson<List<List<List<Int>>>>("creeperBeamSolutions.json").map {
            BeamPair(BlockPos(it[0][0], it[0][1], it[0][2]), BlockPos(it[1][0], it[1][1], it[1][2]))
        }
    }

    private val currentSolve = CopyOnWriteArrayList<BeamPair>()

    private var inCreeperBeams = false
    private var roomCenter = BlockPos.ZERO
    private var rotation = 0

    private val colorPool = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.ORANGE, Color.WHITE, Color.MAGENTA
    )


    fun onStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (! inCreeperBeams) return
        if (event.room.name != "Creeper Beams") return
        if (event.newState != RoomState.GREEN) return
        reset()
    }

    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Creeper Beams") return
        inCreeperBeams = true
        rotation = 360 - event.room.rotation !! + 180
        roomCenter = event.room.centerPos
        solve()
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! inCreeperBeams) return
        currentSolve.forEach { (start, end, color) ->
            val startBlock = WorldUtils.getBlockAt(start)
            val endBlock = WorldUtils.getBlockAt(end)

            if (startBlock != Blocks.SEA_LANTERN && endBlock != Blocks.SEA_LANTERN) return@forEach

            Render3D.renderBlock(ctx, start, color, phase = phase.value)
            Render3D.renderBlock(ctx, end, color, phase = phase.value)
            if (renderLines.value) Render3D.renderLine(ctx, start.center, end.center, color)
        }
    }

    private fun solve() {
        currentSolve.clear()
        if (beamSolutions.isEmpty()) return

        var colorIndex = 0

        beamSolutions.forEach { beam ->
            if (colorIndex >= colorPool.size) colorIndex = 0

            val startPos = ScanUtils.getRealCoord(beam.start, roomCenter, rotation)
            val endPos = ScanUtils.getRealCoord(beam.end, roomCenter, rotation)

            if (! isBeamBlock(WorldUtils.getBlockAt(startPos)) || ! isBeamBlock(WorldUtils.getBlockAt(endPos))) return@forEach

            currentSolve.add(BeamPair(startPos, endPos, colorPool[colorIndex ++]))
        }
    }

    private fun isBeamBlock(block: Block?) = block.equalsOneOf(Blocks.PRISMARINE, Blocks.SEA_LANTERN)

    fun reset() {
        inCreeperBeams = false
        currentSolve.clear()
        roomCenter = BlockPos.ZERO
        rotation = 0
    }
}