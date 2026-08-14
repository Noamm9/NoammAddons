package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.icefillColor
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.Render3D.renderLine
import com.github.noamm9.utils.render.RenderContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.*
import kotlin.system.measureTimeMillis

object IceFillSolver: PuzzleSolver {
    override val enabled get() = PuzzleSolvers.icefill.value
    private var puzzles = CopyOnWriteArraySet<IceFillPuzzle>()

    override fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Ice Fill") return
        NoammAddons.scope.launch {
            val time = measureTimeMillis { solve(event.room.centerPos, 360 - event.room.rotation !!) }
            ChatUtils.modMessage("&bIce Fill took ${time}ms to solve")
        }
    }

    override fun onRenderWorld(ctx: RenderContext) = puzzles.forEach { it.draw(ctx) }
    override fun reset() = puzzles.clear()

    private suspend fun solve(center: BlockPos, rotation: Int) = withContext(Dispatchers.Default) {
        val checkpoints = listOf(
            ScanUtils.getRealCoord(BlockPos(0, 69, - 8), center, rotation),
            ScanUtils.getRealCoord(BlockPos(0, 70, - 3), center, rotation),
            ScanUtils.getRealCoord(BlockPos(0, 71, 4), center, rotation),
            ScanUtils.getRealCoord(BlockPos(0, 71, 11), center, rotation)
        )

        reset()

        val allIceBlocks = mutableSetOf<BlockPos>()

        for (dx in - 22 .. 22) for (dz in - 22 .. 22) for (dy in 68 .. 73) {
            val pos = center.offset(dx, dy - center.y, dz)
            val state = WorldUtils.getStateAt(pos)

            if (! state.`is`(Blocks.ICE) && ! state.`is`(Blocks.PACKED_ICE)) continue
            if (! WorldUtils.getStateAt(pos.above()).isAir) continue

            allIceBlocks.add(pos.above())
        }

        if (allIceBlocks.isEmpty()) return@withContext

        val clusters = mutableListOf<MutableSet<BlockPos>>()
        val visited = mutableSetOf<BlockPos>()

        for (pos in allIceBlocks) {
            if (pos in visited) continue

            val cluster = mutableSetOf<BlockPos>()
            val queue = ArrayDeque<BlockPos>()
            queue.add(pos)
            visited.add(pos)

            while (! queue.isEmpty()) {
                val current = queue.removeFirst()
                cluster.add(current)
                for (dir in Direction.Plane.HORIZONTAL) {
                    val next = current.relative(dir)
                    if (next in allIceBlocks && next !in visited) {
                        visited.add(next)
                        queue.add(next)
                    }
                }
            }

            clusters.add(cluster)
        }

        clusters.sortBy { cluster -> cluster.minOf { it.distSqr(checkpoints[0]) } }

        for ((i, cluster) in clusters.withIndex()) {
            if (i >= 3) break

            val spaces = cluster.toHashSet()
            val start = spaces.minBy { it.distSqr(checkpoints[i]) }
            val end = spaces.minBy { it.distSqr(checkpoints[i + 1]) }

            val puzzle = IceFillPuzzle(spaces, start, end).solve()

            if (puzzle.path.isNotEmpty()) puzzles.add(puzzle)
            else ChatUtils.modMessage("§cIceFill: Failed to find path for Puzzle $i.")
        }
    }

    private class IceFillPuzzle(val spaces: HashSet<BlockPos>, val start: BlockPos, val end: BlockPos) {
        var path = mutableListOf<BlockPos>()
            private set

        private val graph = spaces.associateWith { pos ->
            Direction.Plane.HORIZONTAL.filter { pos.relative(it) in spaces }
        }

        private val NO_WALL_PENALTY = 5
        private val DOUBLE_TURN_PENALTY = 2
        private val TURN_PENALTY = 1

        private fun dirBetween(a: BlockPos, b: BlockPos) = when {
            b.x > a.x -> Direction.EAST
            b.x < a.x -> Direction.WEST
            b.z > a.z -> Direction.SOUTH
            else -> Direction.NORTH
        }

        private fun isBackedByWall(pos: BlockPos, dir: Direction) = pos.relative(dir) !in spaces

        private fun remainingConnected(current: BlockPos, visited: Set<BlockPos>): Boolean {
            val remaining = spaces.size - visited.size + 1
            if (remaining <= 1) return true

            val seen = HashSet<BlockPos>(remaining)
            val stack = ArrayDeque<BlockPos>()
            stack.add(current)
            seen.add(current)

            while (stack.isNotEmpty()) {
                val cur = stack.removeLast()
                for (dir in graph[cur] ?: emptyList()) {
                    val n = cur.relative(dir)
                    if ((n == current || n !in visited) && seen.add(n)) stack.add(n)
                }
            }
            return seen.size == remaining
        }

        private fun pathCost(fullPath: List<BlockPos>): Int {
            if (fullPath.size < 3) return 0
            var total = 0
            var prevDir: Direction? = null
            var prevWasTurn = false
            for (i in 1 until fullPath.size) {
                val dir = dirBetween(fullPath[i - 1], fullPath[i])
                if (prevDir != null && dir != prevDir) {
                    total += TURN_PENALTY
                    if (! isBackedByWall(fullPath[i - 1], prevDir)) total += NO_WALL_PENALTY
                    if (prevWasTurn) total += DOUBLE_TURN_PENALTY
                    prevWasTurn = true
                }
                else prevWasTurn = false
                prevDir = dir
            }
            return total
        }

        private fun search(stopAtFirst: Boolean, costBound: Int): List<BlockPos>? {
            var bestCost = costBound
            var bestPath: List<BlockPos>? = null

            val visited = mutableSetOf(start)
            val tempPath = ArrayList<BlockPos>(spaces.size).apply { add(start) }

            fun dfs(current: BlockPos, lastDir: Direction?, lastWasTurn: Boolean, cost: Int): Boolean {
                if (cost >= bestCost) return false

                if (visited.size == spaces.size) {
                    if (current == end) {
                        bestCost = cost
                        bestPath = tempPath.toList()
                        return stopAtFirst
                    }
                    return false
                }
                if (current == end) return false

                val candidates = (graph[current] ?: emptyList())
                    .map { it to current.relative(it) }
                    .filter { (_, pos) -> pos !in visited }
                    .sortedWith(
                        compareBy(
                            { (_, pos) -> (graph[pos] ?: emptyList()).count { pos.relative(it) !in visited } },
                            { (dir, _) -> if (dir == lastDir) 0 else 1 }
                        )
                    )

                for ((dir, nextPos) in candidates) {
                    var stepCost = cost
                    var turned = false
                    if (lastDir != null && dir != lastDir) {
                        turned = true
                        stepCost += TURN_PENALTY
                        if (! isBackedByWall(current, lastDir)) stepCost += NO_WALL_PENALTY
                        if (lastWasTurn) stepCost += DOUBLE_TURN_PENALTY
                    }
                    if (stepCost >= bestCost) continue

                    visited.add(nextPos)
                    tempPath.add(nextPos)

                    val stop = remainingConnected(nextPos, visited) && dfs(nextPos, dir, turned, stepCost)

                    tempPath.removeAt(tempPath.size - 1)
                    visited.remove(nextPos)

                    if (stop) return true
                }
                return false
            }

            dfs(start, null, false, 0)
            return bestPath
        }

        fun solve(): IceFillPuzzle {
            if (start !in spaces || end !in spaces) return this
            if ("ice" in NoammAddons.debugFlags) {
                fun BlockPos.string() = "(x:$x, z:$z)"
                ChatUtils.debug("ice", "spaces=${spaces.joinToString(",") { it.string() }} start=${start.string()} end=${end.string()}")
            }

            val fallback = search(stopAtFirst = true, costBound = Int.MAX_VALUE) ?: return this

            val optimized = search(
                stopAtFirst = false,
                costBound = pathCost(fallback),
            )

            path = (optimized ?: fallback).toMutableList()
            return this
        }

        fun draw(ctx: RenderContext) = path.forEachIndexed { index, pos ->
            if (index == 0) return@forEachIndexed
            val prev = path[index - 1]
            ctx.renderLine(
                Vec3(prev.x + 0.5, prev.y + 0.01, prev.z + 0.5),
                Vec3(pos.x + 0.5, pos.y + 0.01, pos.z + 0.5),
                icefillColor.value,
                thickness = 5f
            )
        }
    }
}