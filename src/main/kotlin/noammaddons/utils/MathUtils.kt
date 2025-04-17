package noammaddons.utils

import net.minecraft.init.Blocks
import net.minecraft.util.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.PlayerUtils.getEyePos
import java.awt.Color
import kotlin.math.*

object MathUtils {
    data class Rotation(var yaw: Float, var pitch: Float)

    /**
     * Checks if a given coordinate is inside a specified 3D box.
     * The box is defined by any two opposite corners, regardless of their order.
     *
     * @param coord The coordinate to check.
     * @param corner1 One corner of the box.
     * @param corner2 The opposite corner of the box.
     * @return True if the coordinate is inside the box, false otherwise.
     */
    fun isCoordinateInsideBox(coord: Vec3, corner1: Vec3, corner2: Vec3): Boolean {
        val minX = minOf(corner1.xCoord, corner2.xCoord)
        val maxX = maxOf(corner1.xCoord, corner2.xCoord)
        val minY = minOf(corner1.yCoord, corner2.yCoord)
        val maxY = maxOf(corner1.yCoord, corner2.yCoord)
        val minZ = minOf(corner1.zCoord, corner2.zCoord)
        val maxZ = maxOf(corner1.zCoord, corner2.zCoord)

        val x = coord.xCoord in minX .. maxX
        val y = coord.yCoord in minY .. maxY
        val z = coord.zCoord in minZ .. maxZ
        return x && y && z
    }

    fun isCoordinateInsideBox(pos: BlockPos, corner1: BlockPos, corner2: BlockPos) = isCoordinateInsideBox(pos.toVec(), corner1.toVec(), corner2.toVec())

    fun getAllBlocksBetween(start: BlockPos, end: BlockPos): List<BlockPos> {
        val minX = minOf(start.x, end.x)
        val maxX = maxOf(start.x, end.x)
        val minY = minOf(start.y, end.y)
        val maxY = maxOf(start.y, end.y)
        val minZ = minOf(start.z, end.z)
        val maxZ = maxOf(start.z, end.z)

        val positions = mutableListOf<BlockPos>()
        for (x in minX .. maxX) {
            for (y in minY .. maxY) {
                for (z in minZ .. maxZ) {
                    positions.add(BlockPos(x, y, z))
                }
            }
        }
        return positions
    }

    /**
     * Calculates the distance between two points in a 3D space using Vec3.
     * @param vec1 The first point as a Vec3.
     * @param vec2 The second point as a Vec3.
     * @return The distance between the two points.
     */
    fun distance3D(vec1: Vec3, vec2: Vec3) = vec1.distanceTo(vec2)
    fun distance3D(pos1: BlockPos, pos2: BlockPos): Double {
        val delta = pos1.subtract(pos2).toVec()
        return sqrt(delta.xCoord.pow(2) + delta.yCoord.pow(2) + delta.zCoord.pow(2))
    }


    /**
     * Calculates the distance between two points in a 2D space (ignoring the Y coordinate) using Vec3.
     * @param vec1 The first point as a Vec3.
     * @param vec2 The second point as a Vec3.
     * @return The distance between the two points in 2D space.
     */
    fun distance2D(vec1: Vec3, vec2: Vec3): Double {
        val deltaX = vec1.xCoord - vec2.xCoord
        val deltaZ = vec1.zCoord - vec2.zCoord
        return sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }

    fun distance2D(pos1: BlockPos, pos2: BlockPos): Double {
        val deltaX = pos1.x - pos2.x
        val deltaZ = pos1.z - pos2.z
        return sqrt((deltaX * deltaX + deltaZ * deltaZ).toDouble())
    }

    fun normalizeYaw(yaw: Float): Float {
        var result = yaw
        while (result >= 180) result -= 360
        while (result < - 180) result += 360
        return result
    }

    fun normalizePitch(pitch: Float): Float {
        var result = pitch
        while (result >= 90) result -= 180
        while (result < - 90) result += 180
        return result
    }

    fun calcYawPitch(blockPos: Vec3, playerPos: Vec3 = getEyePos()): Rotation {
        val delta = blockPos.subtract(playerPos)
        val yaw = - atan2(delta.xCoord, delta.zCoord) * (180 / PI)
        val pitch = - atan2(delta.yCoord, sqrt(delta.xCoord * delta.xCoord + delta.zCoord * delta.zCoord)) * (180 / PI)
        return Rotation(yaw.toFloat(), pitch.toFloat())
    }

    fun getBlockFromLook(rot: Rotation, maxDistance: Int, eyeX: Double, eyeY: Double, eyeZ: Double): BlockPos? {
        val radYaw = Math.toRadians(rot.yaw.toDouble())
        val radPitch = Math.toRadians(rot.pitch.toDouble())

        val dirX = - sin(radYaw) * cos(radPitch)
        val dirY = - sin(radPitch)
        val dirZ = cos(radYaw) * cos(radPitch)

        var x = eyeX
        var y = eyeY
        var z = eyeZ

        val stepSize = 0.05 // Small increments for precision
        for (i in 0 until ((maxDistance.toDouble() / stepSize).toInt())) {
            x += dirX * stepSize
            y += dirY * stepSize
            z += dirZ * stepSize

            val block = getBlockAt(x, y, z)
            if (block == Blocks.air) continue

            return BlockPos(x, y, z)
        }

        return null
    }

    @JvmStatic
    fun interpolate(prev: Number, newPos: Number, partialTicks: Number): Double {
        return prev.toDouble() + (newPos.toDouble() - prev.toDouble()) * partialTicks.toDouble()
    }

    fun interpolateColor(color1: Color, color2: Color, value: Float): Color {
        return Color(
            interpolate(color1.red, color2.red, value).toInt(),
            interpolate(color1.green, color2.green, value).toInt(),
            interpolate(color1.blue, color2.blue, value).toInt()
        )
    }

    fun interpolateYaw(startYaw: Float, targetYaw: Float, progress: Float): Float {
        var delta = (targetYaw - startYaw) % 360

        if (delta > 180) delta -= 360
        if (delta < - 180) delta += 360

        return (startYaw + delta * progress)
    }

    fun interpolatePitch(startPitch: Float, targetPitch: Float, progress: Float): Float {
        var delta = (targetPitch - startPitch)

        // Clamp the delta within the valid pitch range (-90 to 90)
        if (delta > 90) delta = 90f
        if (delta < - 90) delta = - 90f

        return (startPitch + delta * progress)
    }


    fun BlockPos.add(x: Number = 0.0, y: Number = 0.0, z: Number = 0.0) = add(x.toDouble(), y.toDouble(), z.toDouble())

    fun Vec3.floor() = Vec3(floor(xCoord), floor(yCoord), floor(zCoord))
    fun Vec3.add(x: Number = 0.0, y: Number = 0.0, z: Number = 0.0) = add(Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
    fun Vec3i.destructured() = listOf(x, y, z)
    fun Vec3.destructured() = listOf(xCoord, yCoord, zCoord)
}