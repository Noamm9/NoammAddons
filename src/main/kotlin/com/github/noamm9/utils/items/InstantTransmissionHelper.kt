package com.github.noamm9.utils.items

import com.github.noamm9.NoammAddons
import com.github.noamm9.mixin.ILocalPlayer
import com.github.noamm9.utils.MathUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.math.floor
import kotlin.math.roundToInt

object InstantTransmissionHelper {
    private const val EYE_HEIGHT = 1.62
    private const val SNEAK_OFFSET = 0.08

    private const val RAY_TRACE_STEPS = 1000.0

    fun predictTeleport(distance: Double, startPos: Vec3, yaw: Float, pitch: Float): Vec3? {
        val player = NoammAddons.mc.player ?: return null

        val eyeHeight = EYE_HEIGHT - if ((player as ILocalPlayer).isSneakingServer) SNEAK_OFFSET else .0
        var currentPosition = Vec3(startPos.x, startPos.y + eyeHeight, startPos.z)

        val direction = MathUtils.getLookVec(yaw, pitch)
        val stepVector = direction.scale(1.0 / RAY_TRACE_STEPS)

        for (step in 0 .. (distance * RAY_TRACE_STEPS).roundToInt()) {
            val isFullBlockInterval = step % RAY_TRACE_STEPS == 0.0

            if (isFullBlockInterval && isSolidBlockInPath(currentPosition)) {
                currentPosition = currentPosition.subtract(stepVector.scale(RAY_TRACE_STEPS))

                return if (step == 0 || isSolidBlockInPath(currentPosition)) null
                else createLandingVector(currentPosition)
            }

            if (isPartialBlockInPath(currentPosition)) {
                currentPosition = currentPosition.subtract(stepVector.scale(RAY_TRACE_STEPS))
                break
            }

            currentPosition = currentPosition.add(stepVector)
        }

        return if (isSolidBlockInPath(currentPosition)) null
        else createLandingVector(currentPosition)
    }

    private fun isSolidBlockInPath(position: Vec3): Boolean {
        val isIgnoredAtFeet = isPassableBlock(position)
        val isIgnoredAtHead = isPassableBlock(position.add(0.0, 1.0, 0.0))
        return ! isIgnoredAtFeet || ! isIgnoredAtHead
    }

    private fun isPartialBlockInPath(position: Vec3): Boolean {
        val isPartialAtFeet = isPartialSolidBlock(position) && positionIsInBoundingBox(position)
        val isPartialAtHead = isPartialSolidBlock(position.add(0.0, 1.0, 0.0)) && positionIsInBoundingBox(position.add(0.0, 1.0, 0.0))
        return isPartialAtFeet || isPartialAtHead
    }

    private fun isPassableBlock(vec: Vec3): Boolean {
        val level = NoammAddons.mc.level ?: return false
        val blockPos = BlockPos(floor(vec.x).toInt(), floor(vec.y).toInt(), floor(vec.z).toInt())
        val state = level.getBlockState(blockPos)
        return PASSABLE_BLOCKS.contains(state.block) || state.getCollisionShape(level, blockPos).isEmpty
    }

    private fun isPartialSolidBlock(vec: Vec3): Boolean {
        val level = NoammAddons.mc.level ?: return false
        val blockPos = BlockPos(floor(vec.x).toInt(), floor(vec.y).toInt(), floor(vec.z).toInt())
        val state = level.getBlockState(blockPos)

        if (PASSABLE_BLOCKS.contains(state.block)) return false

        val shape = state.getCollisionShape(level, blockPos)
        if (shape.isEmpty) return false

        val bounds = shape.bounds()
        return bounds.maxX - bounds.minX < 1.0 || bounds.maxY - bounds.minY < 1.0 || bounds.maxZ - bounds.minZ < 1.0
    }

    private fun positionIsInBoundingBox(vec: Vec3): Boolean {
        val level = NoammAddons.mc.level ?: return false
        val blockPos = BlockPos(floor(vec.x).toInt(), floor(vec.y).toInt(), floor(vec.z).toInt())
        val state = level.getBlockState(blockPos)

        val shape = state.getCollisionShape(level, blockPos, CollisionContext.empty())
        if (shape.isEmpty) return false

        val boundingBox = shape.bounds().move(blockPos)
        return boundingBox.contains(vec)
    }

    private fun createLandingVector(finalPos: Vec3): Vec3 {
        return Vec3(floor(finalPos.x) + 0.5, floor(finalPos.y), floor(finalPos.z) + 0.5)
    }

    private val PASSABLE_BLOCKS = setOf(
        Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR,
        Blocks.WATER, Blocks.LAVA, Blocks.TALL_GRASS, Blocks.SHORT_GRASS,
        Blocks.FERN, Blocks.LARGE_FERN, Blocks.DANDELION, Blocks.POPPY,
        Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP,
        Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
        Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.WITHER_ROSE, Blocks.SUNFLOWER,
        Blocks.TORCH, Blocks.WALL_TORCH, Blocks.REDSTONE_WIRE, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
        Blocks.SNOW, Blocks.VINE, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
        Blocks.SUGAR_CANE, Blocks.KELP, Blocks.LILY_PAD, Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT,
        Blocks.BEETROOTS, Blocks.SWEET_BERRY_BUSH, Blocks.DEAD_BUSH
    )
}