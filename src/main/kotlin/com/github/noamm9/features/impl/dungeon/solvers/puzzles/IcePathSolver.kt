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
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Point
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

object IcePathSolver {
    private data class PathSegment(val start: Vec3, val end: Vec3)

    private const val GRID_ORIGIN_X = -8
    private const val GRID_ORIGIN_Z = -8
    private const val GRID_SIZE = 19
    private const val END_X = 7
    private const val END_Y = 18

    private val currentSolution = ConcurrentLinkedQueue<PathSegment>()
    private var silverfishEntity: Silverfish? = null
    private var lastGridPos: Point? = null
    private var inPath = false
    private var roomCenter: BlockPos? = null
    private var roomRotation: Int = 0

    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Ice Path") return
        inPath = true
        roomCenter = event.room.centerPos
        roomRotation = 360 - event.room.rotation!!
    }

    fun onTick() {
        if (!inPath) return

        if (silverfishEntity == null || silverfishEntity?.isDeadOrDying == true) {
            silverfishEntity = mc.level?.getEntitiesOfClass(Silverfish::class.java, mc.player?.boundingBox?.inflate(30.0) ?: return)?.firstOrNull()
        }

        val fish = silverfishEntity ?: return
        if (fish.isDeadOrDying) {
            if (currentSolution.size <= 1) currentSolution.clear()
            return
        }

        val center = roomCenter ?: return
        val fishGridPos = worldToGrid(fish.blockPosition(), center) ?: return

        if (fishGridPos == lastGridPos) return
        lastGridPos = fishGridPos

        if (currentSolution.isNotEmpty()) {
            val first = currentSolution.peek()
            val dist = abs(fish.x - first.end.x) + abs(fish.z - first.end.z)
            if (dist <= 0.8) {
                currentSolution.poll()
                return
            }

            if (isOnSegment(fish.x, fish.z, first)) return
        }

        recalculateFromBFS(fishGridPos, center)
    }

    private fun isOnSegment(x: Double, z: Double, seg: PathSegment): Boolean {
        val minX = minOf(seg.start.x, seg.end.x) - 0.8
        val maxX = maxOf(seg.start.x, seg.end.x) + 0.8
        val minZ = minOf(seg.start.z, seg.end.z) - 0.8
        val maxZ = maxOf(seg.start.z, seg.end.z) + 0.8
        return x in minX..maxX && z in minZ..maxZ
    }

    private fun recalculateFromBFS(fishGridPos: Point, center: BlockPos) {
        val grid = buildGrid(center) ?: return
        val path = solve(grid, fishGridPos.x, fishGridPos.y, END_X, END_Y)
        if (path.size < 2) return

        currentSolution.clear()
        path.zipWithNext().forEach { (p1, p2) ->
            val pos1 = gridToWorld(p1, center)
            val pos2 = gridToWorld(p2, center)
            currentSolution.add(PathSegment(pos1, pos2))
        }
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (currentSolution.isEmpty()) return

        currentSolution.forEachIndexed { idx, seg ->
            Render3D.renderLine(ctx, seg.start, seg.end, if (idx == 0) icePathFirstColor.value else icePathColor.value, thickness = 3f)
        }
    }

    private fun buildGrid(center: BlockPos): Array<IntArray>? {
        val grid = Array(GRID_SIZE) { IntArray(GRID_SIZE) }
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val relPos = BlockPos(GRID_ORIGIN_X + col, 67, GRID_ORIGIN_Z + row)
                val worldPos = ScanUtils.getRealCoord(relPos, center, roomRotation)
                val block = WorldUtils.getStateAt(worldPos).block
                grid[row][col] = if (block != Blocks.AIR && block != Blocks.CAVE_AIR) 1 else 0
            }
        }
        return grid
    }

    private fun worldToGrid(pos: BlockPos, center: BlockPos): Point? {
        val dx = pos.x - center.x
        val dz = pos.z - center.z
        val relPos = BlockPos(dx, 0, dz).rotate(-roomRotation)
        val col = relPos.x - GRID_ORIGIN_X
        val row = relPos.z - GRID_ORIGIN_Z
        if (col !in 0 until GRID_SIZE || row !in 0 until GRID_SIZE) return null
        return Point(col, row)
    }

    private fun gridToWorld(point: Point, center: BlockPos): Vec3 {
        val relPos = BlockPos(GRID_ORIGIN_X + point.x, 66, GRID_ORIGIN_Z + point.y)
        val worldPos = ScanUtils.getRealCoord(relPos, center, roomRotation)
        return Vec3(worldPos.x + 0.5, 67.5, worldPos.z + 0.5)
    }

    private fun solve(grid: Array<IntArray>, startX: Int, startY: Int, endX: Int, endY: Int): List<Point> {
        val startPoint = Point(startX, startY)
        val queue = ArrayDeque<Point>()
        val parent = Array(grid.size) { arrayOfNulls<Point>(grid[0].size) }

        queue.addLast(startPoint)
        parent[startY][startX] = startPoint

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()

            for (dir in Direction.Plane.HORIZONTAL) {
                val next = slide(grid, parent, curr, dir) ?: continue
                queue.addLast(next)
                parent[next.y][next.x] = Point(curr.x, curr.y)

                if (next.x == endX && next.y == endY) {
                    val path = mutableListOf<Point>()
                    var tmp: Point? = next
                    while (tmp != null && tmp != startPoint) {
                        path.add(tmp)
                        tmp = parent[tmp.y][tmp.x]
                    }
                    path.add(startPoint)
                    path.reverse()
                    return path
                }
            }
        }
        return emptyList()
    }

    private fun slide(grid: Array<IntArray>, parent: Array<Array<Point?>>, from: Point, dir: Direction): Point? {
        val dx = dir.stepX
        val dz = dir.stepZ
        var i = 1
        while (from.x + i * dx in grid[0].indices && from.y + i * dz in grid.indices && grid[from.y + i * dz][from.x + i * dx] != 1) {
            i++
        }
        i--
        if (i == 0) return null
        val nx = from.x + i * dx
        val ny = from.y + i * dz
        return if (parent[ny][nx] != null) null else Point(nx, ny)
    }

    fun reset() {
        inPath = false
        silverfishEntity = null
        lastGridPos = null
        currentSolution.clear()
        roomCenter = null
    }
}
