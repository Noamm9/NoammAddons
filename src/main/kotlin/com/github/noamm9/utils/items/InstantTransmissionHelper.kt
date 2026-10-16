package com.github.noamm9.utils.items

import com.github.noamm9.NoammAddons.mc
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.piston.MovingPistonBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.concurrent.*
import kotlin.math.floor

// aotv prediction adapted from Shizo by @ImJoyler
object InstantTransmissionHelper {
    private const val EYE_HEIGHT = 1.62
    private val stateCells = ConcurrentHashMap<BlockState, CollisionCell>()

    private class CollisionCell(
        val boxes: List<AABB>,
        val passable: Boolean,
        val bottomSlab: Boolean = false,
        val landingShape: Boolean = false,
        val pane: Boolean = false,
        val bars: Boolean = false,
        val exact: Boolean = false
    ) {
        val fullCube = boxes.any { it.minX == 0.0 && it.minY == 0.0 && it.minZ == 0.0 && it.maxX == 1.0 && it.maxY == 1.0 && it.maxZ == 1.0 }
    }

    private class LevelWorld(private val level: BlockGetter) {
        private val cells = HashMap<BlockPos, CollisionCell>()
        private val bodies = HashMap<AABB, Boolean>()

        fun cell(pos: BlockPos) = cells.getOrPut(pos) { levelCell(level, pos) }
        fun noCollision(body: AABB) = bodies.getOrPut(body) { free(body) }

        private fun free(body: AABB): Boolean {
            for (x in floor(body.minX).toInt() .. floor(body.maxX - 1e-7).toInt())
                for (y in floor(body.minY).toInt() - 1 .. floor(body.maxY - 1e-7).toInt())
                    for (z in floor(body.minZ).toInt() .. floor(body.maxZ - 1e-7).toInt())
                        for (b in cell(BlockPos(x, y, z)).boxes)
                            if (body.minX < x + b.maxX - 1e-7 && body.maxX > x + b.minX + 1e-7 &&
                                body.minY < y + b.maxY - 1e-7 && body.maxY > y + b.minY + 1e-7 &&
                                body.minZ < z + b.maxZ - 1e-7 && body.maxZ > z + b.minZ + 1e-7
                            ) return false
            return true
        }
    }

    fun predictTeleport(distance: Double, startPos: Vec3, yaw: Float, pitch: Float): Vec3? {
        val world = LevelWorld(mc.level ?: return null)
        val dir = Vec3.directionFromRotation(pitch, yaw)

        val startBlock = BlockPos.containing(startPos)
        val startCell = world.cell(startBlock)
        if ((startCell.bars && startPos.y == floor(startPos.y)) || startCell.boxes.any { it.maxY > 1.0 }) return null

        val eye = startPos.add(0.0, EYE_HEIGHT, 0.0)
        val obstructed = HashMap<BlockPos, Boolean>()
        fun isObstructed(target: BlockPos) = obstructed.getOrPut(target) {
            isLandingObstructed(eye, Vec3.atCenterOf(target), world::cell)
        }

        var k = floor(distance).toInt()
        var pos = startBlock
        var block = startCell
        for (i in 0 .. (distance * 100).toInt()) {
            val t = i / 100.0
            val px = eye.x + dir.x * t
            val py = eye.y + dir.y * t
            val pz = eye.z + dir.z * t
            val bx = floor(px).toInt()
            val by = floor(py).toInt()
            val bz = floor(pz).toInt()
            if (bx != pos.x || by != pos.y || bz != pos.z || i == 0) {
                pos = BlockPos(bx, by, bz)
                block = world.cell(pos)
            }
            if (block.passable || block.fullCube) continue
            val passesShape = (block.bottomSlab || block.pane || block.exact) && block.boxes.none { it.contains(px - bx, py - by, pz - bz) }
            if (! passesShape) {
                k = floor(t).toInt()
                break
            }
        }

        var landing: Vec3? = null
        var closeFloorY: Double? = null
        for (step in 1 .. k) {
            val point = eye.add(dir.scale(step.toDouble()))
            val x = floor(point.x) + 0.5
            var y = floor(point.y)
            val z = floor(point.z) + 0.5
            val target = BlockPos.containing(x, y, z)
            if (isObstructed(target)) break

            closeFloorY?.let { fy ->
                val support = world.cell(BlockPos.containing(x, fy - 1.0, z))
                if (support.passable || ! support.fullCube) closeFloorY = null
            }
            if (closeFloorY?.let { y < it } == true) break

            val below = world.cell(BlockPos.containing(x, y - 1.0, z))
            if (! below.passable && below.fullCube && point.y - y < 0.31 && world.noCollision(bodyBox(x, y, z))) closeFloorY = y
            if (below.passable && world.noCollision(bodyBox(x, y - 1.0, z)) && ! isObstructed(target.below())) y -= 1.0

            val body = bodyBox(x, y, z)
            if (! world.noCollision(body)) {
                if (onlyPaneHeadCollision(body, world::cell)) continue
                break
            }
            landing = Vec3(x, y, z)
        }
        return landing
    }

    private fun levelCell(level: BlockGetter, pos: BlockPos): CollisionCell {
        val state = level.getBlockState(pos)
        if (state.hasOffsetFunction() || state.block is MovingPistonBlock) return stateCell(level, pos, state)
        return stateCells.getOrPut(state) { stateCell(level, pos, state) }
    }

    private fun stateCell(level: BlockGetter, pos: BlockPos, state: BlockState): CollisionCell {
        if (state.block is CarpetBlock) return CollisionCell(emptyList(), true)
        val shape = state.getCollisionShape(level, pos)
        return CollisionCell(
            boxes = shape.toAabbs(),
            passable = state.block in PASSABLE_BLOCKS || shape.isEmpty,
            bottomSlab = state.block is SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
            landingShape = state.`is`(Blocks.CHEST) || state.`is`(Blocks.TRAPPED_CHEST) || state.`is`(Blocks.ENDER_CHEST) ||
                state.block is SlabBlock || state.`is`(Blocks.END_PORTAL_FRAME) || state.block is StairBlock,
            pane = state.`is`(Blocks.GLASS_PANE) || state.block is StainedGlassPaneBlock,
            bars = state.block is IronBarsBlock,
            exact = state.block is StairBlock || state.block is FenceBlock
        )
    }

    private fun onlyPaneHeadCollision(body: AABB, cell: (BlockPos) -> CollisionCell): Boolean {
        var found = false
        for (x in floor(body.minX).toInt() .. floor(body.maxX).toInt())
            for (y in floor(body.minY).toInt() - 1 .. floor(body.maxY).toInt())
                for (z in floor(body.minZ).toInt() .. floor(body.maxZ).toInt()) {
                    val pos = BlockPos(x, y, z)
                    val block = cell(pos)
                    for (box in block.boxes) {
                        val placed = box.move(pos)
                        if (! body.intersects(placed)) continue
                        if (! block.pane || placed.minY < body.minY + 1.0) return false
                        found = true
                    }
                }

        return found
    }

    private fun isLandingObstructed(start: Vec3, end: Vec3, cell: (BlockPos) -> CollisionCell): Boolean {
        if (start == end) return false
        val d = end.subtract(start)
        val a = start.add(d.scale(- 1.0e-7))
        val v = end.add(d.scale(1.0e-7)).subtract(a)
        var pos = BlockPos.containing(a)

        val stepX = if (v.x > 0.0) 1 else if (v.x < 0.0) - 1 else 0
        val stepY = if (v.y > 0.0) 1 else if (v.y < 0.0) - 1 else 0
        val stepZ = if (v.z > 0.0) 1 else if (v.z < 0.0) - 1 else 0
        val deltaX = if (stepX == 0) Double.MAX_VALUE else stepX / v.x
        val deltaY = if (stepY == 0) Double.MAX_VALUE else stepY / v.y
        val deltaZ = if (stepZ == 0) Double.MAX_VALUE else stepZ / v.z

        fun firstBoundary(origin: Double, step: Int, delta: Double): Double {
            val f = origin - floor(origin)
            return delta * if (step > 0) 1.0 - f else f
        }

        var tMaxX = firstBoundary(a.x, stepX, deltaX)
        var tMaxY = firstBoundary(a.y, stepY, deltaY)
        var tMaxZ = firstBoundary(a.z, stepZ, deltaZ)

        while (true) {
            val block = cell(pos)
            if (! block.passable && block.fullCube) return true
            if (! block.passable && block.landingShape && block.boxes.any { rayIntersects(start, d, it.move(pos)) }) return true
            if (tMaxX > 1.0 && tMaxY > 1.0 && tMaxZ > 1.0) return false

            when {
                tMaxX < tMaxY && tMaxX < tMaxZ -> {
                    tMaxX += deltaX
                    pos = pos.offset(stepX, 0, 0)
                }

                tMaxY < tMaxZ -> {
                    tMaxY += deltaY
                    pos = pos.offset(0, stepY, 0)
                }

                else -> {
                    tMaxZ += deltaZ
                    pos = pos.offset(0, 0, stepZ)
                }
            }
        }
    }

    private fun rayIntersects(start: Vec3, d: Vec3, box: AABB): Boolean {
        var t0 = 0.0
        var t1 = 1.0

        fun clip(origin: Double, delta: Double, low: Double, high: Double): Boolean {
            if (delta == 0.0) return origin in low .. high
            val a = (low - origin) / delta
            val b = (high - origin) / delta
            t0 = maxOf(t0, minOf(a, b))
            t1 = minOf(t1, maxOf(a, b))
            return t0 <= t1
        }

        return clip(start.x, d.x, box.minX, box.maxX) && clip(start.y, d.y, box.minY, box.maxY) && clip(start.z, d.z, box.minZ, box.maxZ)
    }

    private fun bodyBox(x: Double, y: Double, z: Double) = AABB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3)

    private val PASSABLE_BLOCKS by lazy {
        setOf(
            Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR,
            Blocks.WATER, Blocks.LAVA, Blocks.TALL_GRASS, Blocks.SHORT_GRASS,
            Blocks.FERN, Blocks.LARGE_FERN, Blocks.DANDELION, Blocks.POPPY,
            Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.WITHER_ROSE, Blocks.SUNFLOWER,
            Blocks.TORCH, Blocks.WALL_TORCH, Blocks.REDSTONE_WIRE, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
            Blocks.SNOW, Blocks.VINE, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
            Blocks.SUGAR_CANE, Blocks.KELP, Blocks.LILY_PAD, Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT,
            Blocks.BEETROOTS, Blocks.SWEET_BERRY_BUSH, Blocks.DEAD_BUSH, Blocks.LADDER
        )
    }
}