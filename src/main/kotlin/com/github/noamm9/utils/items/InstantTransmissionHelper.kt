package com.github.noamm9.utils.items

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.MathUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CarpetBlock
import net.minecraft.world.level.block.FlowerPotBlock
import net.minecraft.world.level.block.WebBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

object InstantTransmissionHelper {
    private const val EYE_HEIGHT = 1.62

    fun predictTeleport(distance: Double, startPos: Vec3, yaw: Float, pitch: Float): Vec3? {
        val level = mc.level ?: return null
        val start = Vec3(startPos.x, startPos.y + EYE_HEIGHT, startPos.z)
        val teleportVector = raycast(distance, MathUtils.getLookVec(yaw, pitch), start, level) ?: return null

        val predictedEnd = start.add(teleportVector)
        val offsetVec = Vec3(
            predictedEnd.x - roundToCenter(predictedEnd.x),
            predictedEnd.y - (ceil(predictedEnd.y) + EYE_HEIGHT - 1),
            predictedEnd.z - roundToCenter(predictedEnd.z)
        )
        
        val finalEyePos = predictedEnd.subtract(offsetVec)
        return Vec3(finalEyePos.x, finalEyePos.y - EYE_HEIGHT, finalEyePos.z)
    }

    private fun raycast(distance: Double, direction: Vec3, startPos: Vec3, level: Level): Vec3? {
        val xDiagonalOffset = if (direction.x > 0) BlockPos(1, 0, 0) else BlockPos(- 1, 0, 0)
        val zDiagonalOffset = if (direction.z > 0) BlockPos(0, 0, 1) else BlockPos(0, 0, - 1)
        var closeFloorY = Int.MAX_VALUE

        for (offset in 0 .. distance.toInt()) {
            val pos = startPos.add(direction.scale(offset.toDouble()))
            val checkPos = BlockPos.containing(pos)

            if (! isPassable(level, checkPos)) {
                return if (offset == 0) null else direction.scale((offset - 1).toDouble())
            }

            if (! isPassable(level, checkPos.above())) {
                if (offset == 0) {
                    val justAhead = startPos.add(direction.scale(0.2))
                    if ((justAhead.y - floor(justAhead.y)) <= 0.495) continue
                    return null
                }
                return direction.scale((offset - 1).toDouble())
            }

            if (offset != 0 && direction.x < 0 && isBlockFloor(level, checkPos.east()) && isBlockFloor(level, BlockPos.containing(pos.subtract(direction)).offset(zDiagonalOffset))) {
                return direction.scale((offset - 1).toDouble())
            }
            if (offset != 0 && direction.z < 0 && direction.x < 0 && isBlockFloor(level, checkPos.south()) && isBlockFloor(level, BlockPos.containing(pos.subtract(direction)).offset(xDiagonalOffset))) {
                return direction.scale((offset - 1).toDouble())
            }

            if ((isBlockFloor(level, checkPos.below()) || (isBlockFloor(level, checkPos.below().offset(xDiagonalOffset)) && isBlockFloor(level, checkPos.below().offset(zDiagonalOffset)))) && (pos.y - floor(pos.y)) < 0.31) {
                closeFloorY = checkPos.y - 1
            }

            if (closeFloorY == checkPos.y) return direction.scale((offset - 1).toDouble())
        }

        return if (! isBlockFloor(level, BlockPos.containing(startPos.add(direction.scale(distance)).subtract(0.0, 1.0, 0.0)))) {
            direction.scale(distance).subtract(0.0, 1.0, 0.0)
        }
        else direction.scale(distance)
    }

    private fun isPassable(level: Level, blockPos: BlockPos): Boolean {
        val blockState = level.getBlockState(blockPos)
        if (blockState.isAir) return true
        val block = blockState.block
        val shape = blockState.getCollisionShape(level, blockPos)

        return shape.isEmpty ||
            block is CarpetBlock || block is FlowerPotBlock || block is WebBlock ||
            (block == Blocks.SNOW && blockState.getValue(BlockStateProperties.LAYERS) <= 3)
    }

    private fun isBlockFloor(level: Level, blockPos: BlockPos): Boolean {
        val blockState = level.getBlockState(blockPos)
        val shape = blockState.getCollisionShape(level, blockPos)
        if (shape.isEmpty) return false
        return shape.bounds().maxY >= 1 || blockState.block == Blocks.MUD
    }

    private fun roundToCenter(input: Double) = round(input - 0.5) + 0.5
}