package noammaddons.features.impl.general.teleport

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noammaddons.NoammAddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.MathUtils
import noammaddons.utils.MathUtils.floor
import noammaddons.utils.ServerPlayer.player
import kotlin.math.*

object InstantTransmissionPredictor {
    // Blocks that can be passed through without stopping (air, liquids, grass, etc.).
    private val PASSABLE_BLOCK_IDS = setOf(
        0, 51, 8, 9, 10, 11, 171, 331, 39, 40, 115, 132, 77, 143, 66, 27, 28, 157,
        175, 31, 6, 38, 55, 75, 76, 69, 149, 150, 93, 94, 65, 106, 37, 50, 140, 105,
        59, 141, 142, 32, 83
    )

    // Blocks that are solid but have a non-full bounding box (slabs, etc.).
    private val PARTIAL_SOLID_BLOCK_IDS = setOf(44, 182, 126)

    // Special case blocks that require specific handling (ladders, vines).
    private val SPECIAL_COLLISION_BLOCK_IDS = setOf<Int>()

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
        val isSpecialAtFeet = isSpecialCollisionBlock(position)
        val isSpecialAtHead = isSpecialCollisionBlock(position.copy().addY(1.0))

        // Special blocks (ladders, vines) don't count as solid barriers at whole-block checks
        if (! isSpecialAtFeet && ! isSpecialAtHead) {
            val isIgnoredAtFeet = isPassableBlock(position)
            val isIgnoredAtHead = isPassableBlock(position.copy().addY(1.0))
            // If either feet or head position is not ignored (i.e., solid), return true
            return ! isIgnoredAtFeet || ! isIgnoredAtHead
        }
        return false
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
    private fun isSpecialCollisionBlock(vec: Vector3) = SPECIAL_COLLISION_BLOCK_IDS.contains(getBlockIdAtVector(vec))

    private fun isPartialSolidBlock(vec: Vector3): Boolean {
        val blockId = getBlockIdAtVector(vec)
        // A block is considered a partial solid if it's not a standard passable block
        // AND it's in the designated list of partial solids.
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

    data class Vector3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
        companion object {
            fun fromPitchYaw(pitch: Double, yaw: Double): Vector3 {
                val f = cos(- yaw * 0.017453292 - Math.PI)
                val f1 = sin(- yaw * 0.017453292 - Math.PI)
                val f2 = - cos(- pitch * 0.017453292)
                val f3 = sin(- pitch * 0.017453292)
                return Vector3(f1 * f2, f3, f * f2).normalize()
            }
        }

        fun add(other: Vector3): Vector3 {
            this.x += other.x
            this.y += other.y
            this.z += other.z
            return this
        }

        fun addY(value: Double): Vector3 {
            this.y += value
            return this
        }

        fun multiply(factor: Double): Vector3 {
            this.x *= factor
            this.y *= factor
            this.z *= factor
            return this
        }

        fun normalize(): Vector3 {
            val length = length()
            if (length != 0.0) multiply(1.0 / length)
            return this
        }

        fun length(): Double = sqrt(x * x + y * y + z * z)

        fun dotProduct(vector3: Vector3): Double {
            return (x * vector3.x) + (y * vector3.y) + (z * vector3.z)
        }

        fun getAngleRad(vector3: Vector3): Double {
            return acos(dotProduct(vector3) / (length() * vector3.length()))
        }

        fun getAngleDeg(vector3: Vector3): Double {
            return 180 / Math.PI * getAngleRad(vector3)
        }
    }
}