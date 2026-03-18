package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.icePathColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.icePathFirstColor
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils.rotate
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.monster.Silverfish
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

object IcePathSolver {
    private data class PathSegment(val start: Vec3, val end: Vec3)
    private data class GridPos(val x: Int, val z: Int)

    private var inPath = false
    private var roomCenter: BlockPos? = null
    private var roomRotation: Int = 0
    private var grid: Array<IntArray>? = null

    private val currentSolution = ConcurrentLinkedQueue<PathSegment>()
    private var silverfish: Silverfish? = null
    private var lastGridPos: GridPos? = null

    private const val GRID_SIZE = 19
    private const val MIN_OFFSET = - 8

    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Ice Path") return

        inPath = true
        roomCenter = event.room.centerPos
        roomRotation = 360 - (event.room.rotation ?: return)
        grid = buildGrid(event.room.centerPos)
    }

    fun reset() {
        inPath = false
        roomCenter = null
        grid = null
        silverfish = null
        lastGridPos = null
        currentSolution.clear()
    }

    fun onTick() {
        if (! inPath) return
        val center = roomCenter ?: return
        var fish = silverfish

        if (fish == null || fish.isRemoved) {
            val aabb = AABB(center.x - 10.0, 67.0, center.z - 10.0, center.x + 10.0, 68.0, center.z + 10.0)
            silverfish = mc.level?.getEntitiesOfClass(Silverfish::class.java, aabb)?.firstOrNull() ?: return
            fish = silverfish
        }

        val fishGridPos = fish?.let { worldToGrid(it.blockPosition(), center) } ?: return
        if (fishGridPos == lastGridPos) return
        lastGridPos = fishGridPos

        if (currentSolution.isNotEmpty()) {
            val currentSegment = currentSolution.peek()
            val dx = abs(fish.x - currentSegment.end.x)
            val dz = abs(fish.z - currentSegment.end.z)
            if (dx + dz <= 0.8) {
                currentSolution.poll()
                return
            }

            if (isOnSegment(fish.x, fish.z, currentSegment)) return
        }

        recalculatePath(fishGridPos, center)
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! inPath || currentSolution.isEmpty() || silverfish?.isRemoved == true) return

        currentSolution.forEachIndexed { index, segment ->
            val color = if (index == 0) icePathFirstColor.value else icePathColor.value
            Render3D.renderLine(ctx, segment.start, segment.end, color, thickness = 5f)
        }
    }

    private fun isOnSegment(x: Double, z: Double, seg: PathSegment): Boolean {
        val padding = 0.8
        val minX = minOf(seg.start.x, seg.end.x) - padding
        val maxX = maxOf(seg.start.x, seg.end.x) + padding
        val minZ = minOf(seg.start.z, seg.end.z) - padding
        val maxZ = maxOf(seg.start.z, seg.end.z) + padding
        return x in minX .. maxX && z in minZ .. maxZ
    }

    private fun recalculatePath(fishPos: GridPos, center: BlockPos) {
        val currentGrid = grid ?: buildGrid(center).also { grid = it }
        val path = solveBFS(currentGrid, fishPos).takeIf { it.size >= 2 } ?: return

        currentSolution.clear()

        path.zipWithNext().forEach { (p1, p2) ->
            val pos1 = gridToWorld(p1, center)
            val pos2 = gridToWorld(p2, center)
            currentSolution.add(PathSegment(pos1, pos2))
        }
    }

    private fun buildGrid(center: BlockPos): Array<IntArray> {
        val newGrid = Array(GRID_SIZE) { IntArray(GRID_SIZE) }

        for (z in 0 until GRID_SIZE) for (x in 0 until GRID_SIZE) {
            val relPos = BlockPos(MIN_OFFSET + x, 67, MIN_OFFSET + z)
            val worldPos = ScanUtils.getRealCoord(relPos, center, roomRotation)
            newGrid[z][x] = if (WorldUtils.getStateAt(worldPos).isAir) 0 else 1
        }
        return newGrid
    }

    private fun worldToGrid(pos: BlockPos, center: BlockPos): GridPos? {
        val dx = pos.x - center.x
        val dz = pos.z - center.z

        val relPos = BlockPos(dx, 0, dz).rotate(- roomRotation)
        val gridX = relPos.x - MIN_OFFSET
        val gridZ = relPos.z - MIN_OFFSET

        if (gridX !in 0 until GRID_SIZE || gridZ !in 0 until GRID_SIZE) return null
        return GridPos(gridX, gridZ)
    }

    private fun gridToWorld(point: GridPos, center: BlockPos): Vec3 {
        val relPos = BlockPos(MIN_OFFSET + point.x, 66, MIN_OFFSET + point.z)
        val worldPos = ScanUtils.getRealCoord(relPos, center, roomRotation)
        return Vec3(worldPos.x + 0.5, 67.5, worldPos.z + 0.5)
    }

    private fun solveBFS(grid: Array<IntArray>, start: GridPos): List<GridPos> {
        val queue = ArrayDeque<GridPos>()
        val parentMap = Array(GRID_SIZE) { arrayOfNulls<GridPos>(GRID_SIZE) }

        queue.addLast(start)
        parentMap[start.z][start.x] = start

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()

            for (dir in Direction.Plane.HORIZONTAL) {
                val next = slide(grid, parentMap, curr, dir) ?: continue

                queue.addLast(next)
                parentMap[next.z][next.x] = curr

                if (next.x == 7 && next.z == 18) {
                    val path = ArrayDeque<GridPos>()
                    var tmp: GridPos? = next

                    while (tmp != null && tmp != start) {
                        path.addFirst(tmp)
                        tmp = parentMap[tmp.z][tmp.x]
                    }
                    path.addFirst(start)
                    return path.toList()
                }
            }
        }
        return emptyList()
    }

    private fun slide(grid: Array<IntArray>, parent: Array<Array<GridPos?>>, from: GridPos, dir: Direction): GridPos? {
        val dx = dir.stepX
        val dz = dir.stepZ
        var steps = 1

        while (true) {
            val nx = from.x + steps * dx
            val nz = from.z + steps * dz

            if (nx !in 0 until GRID_SIZE || nz !in 0 until GRID_SIZE || grid[nz][nx] == 1) break

            steps ++
        }

        steps --
        if (steps == 0) return null

        val finalX = from.x + steps * dx
        val finalZ = from.z + steps * dz

        return if (parent[finalZ][finalX] != null) null else GridPos(finalX, finalZ)
    }
}