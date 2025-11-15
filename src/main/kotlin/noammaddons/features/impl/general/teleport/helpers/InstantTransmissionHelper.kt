package noammaddons.features.impl.general.teleport.helpers

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noammaddons.NoammAddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.MathUtils
import noammaddons.utils.MathUtils.floor
import noammaddons.utils.ServerPlayer.player
import noammaddons.utils.Vector3
import kotlin.math.floor

object InstantTransmissionHelper {
    // Blocks that can be passed through without stopping (air, liquids, grass, etc.).
    private val PASSABLE_BLOCK_IDS = setOf(
        0, 6, 8, 9, 10, 11, 27, 28, 30, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59, 65, 66,
        69, 75, 76, 77, 83, 90, 93, 94, 105, 106, 115, 119, 132, 140, 141, 142, 143, 149,
        150, 157, 171, 175, 331
    )

    // Blocks that are solid but have a non-full bounding box (slabs, etc.).
    private val PARTIAL_SOLID_BLOCK_IDS = setOf(44, 182, 126)

    private const val RAY_TRACE_STEPS = 1000.0
    private const val PLAYER_EYE_HEIGHT = 1.62
    private const val SNEAK_HEIGHT_ADJUSTMENT = 0.08

    fun predictTeleport(distance: Double, startPos: Vec3, rotation: MathUtils.Rotation): Vec3? {
        val eyeHeight = PLAYER_EYE_HEIGHT - if (player.sneaking) SNEAK_HEIGHT_ADJUSTMENT else 0.0
        val currentPosition = Vector3(startPos.xCoord, startPos.yCoord + eyeHeight, startPos.zCoord)
        val direction = Vector3.fromPitchYaw(rotation.pitch.toDouble(), rotation.yaw.toDouble())
        val stepVector = direction.copy().multiply(1.0 / RAY_TRACE_STEPS)

        for (step in 0 .. (distance * RAY_TRACE_STEPS).toInt()) {
            val isFullBlockInterval = step % RAY_TRACE_STEPS == 0.0

            if (isFullBlockInterval && isSolidBlockInPath(currentPosition)) {
                currentPosition.add(stepVector.copy().multiply(- RAY_TRACE_STEPS))

                return if (step == 0 || isSolidBlockInPath(currentPosition)) null
                else createLandingVector(currentPosition)
            }

            if (isPartialBlockInPath(currentPosition)) {
                currentPosition.add(stepVector.copy().multiply(- RAY_TRACE_STEPS))
                break
            }

            currentPosition.add(stepVector)
        }

        return if (isSolidBlockInPath(currentPosition)) null
        else createLandingVector(currentPosition)
    }

    private fun isSolidBlockInPath(position: Vector3): Boolean {
        val isIgnoredAtFeet = isPassableBlock(position)
        val isIgnoredAtHead = isPassableBlock(position.copy().addY(1.0))
        return ! isIgnoredAtFeet || ! isIgnoredAtHead
    }

    private fun isPartialBlockInPath(position: Vector3): Boolean {
        val isPartialAtFeet = isPartialSolidBlock(position) && positionIsInBoundingBox(position)
        val isPartialAtHead = isPartialSolidBlock(position.copy().addY(1.0)) && positionIsInBoundingBox(position.copy().addY(1.0))
        return isPartialAtFeet || isPartialAtHead
    }

    private fun getBlockIdAtVector(vec: Vector3): Int {
        val blockPos = Vec3(vec.x, vec.y, vec.z).floor()
        return getBlockAt(blockPos).getBlockId()
    }

    private fun isPassableBlock(vec: Vector3) = PASSABLE_BLOCK_IDS.contains(getBlockIdAtVector(vec))

    private fun isPartialSolidBlock(vec: Vector3): Boolean {
        val blockId = getBlockIdAtVector(vec)
        return ! PASSABLE_BLOCK_IDS.contains(blockId) && PARTIAL_SOLID_BLOCK_IDS.contains(blockId)
    }

    private fun positionIsInBoundingBox(vec: Vector3): Boolean {
        val pos = Vec3(vec.x, vec.y, vec.z).floor()
        val boundingBox = getBlockAt(pos).getSelectedBoundingBox(mc.theWorld, BlockPos(pos))
        return boundingBox?.isVecInside(pos) ?: false
    }

    private fun createLandingVector(finalPos: Vector3): Vec3 {
        return Vec3(floor(finalPos.x) + 0.5, floor(finalPos.y), floor(finalPos.z) + 0.5)
    }
}