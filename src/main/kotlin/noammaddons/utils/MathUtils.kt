package noammaddons.utils

import net.minecraft.util.Vec3
import noammaddons.utils.PlayerUtils.getEyePos
import java.awt.Color
import kotlin.math.*

object MathUtils {
    /**
     * Checks if a given coordinate is inside a specified 3D box.
     * @param coord The coordinate to check.
     * @param corner1 The coordinates of one corner of the box.
     * @param corner2 The coordinates of the opposite corner of the box.
     * @return True if the coordinate is inside the box, false otherwise.
     */
    fun isCoordinateInsideBox(coord: Vec3, corner1: Vec3, corner2: Vec3): Boolean {
        val min = Vec3(
            corner1.xCoord.coerceAtMost(corner2.xCoord),
            corner1.yCoord.coerceAtMost(corner2.yCoord),
            corner1.zCoord.coerceAtMost(corner2.zCoord)
        )

        val max = Vec3(
            corner1.xCoord.coerceAtLeast(corner2.xCoord),
            corner1.yCoord.coerceAtLeast(corner2.yCoord),
            corner1.zCoord.coerceAtLeast(corner2.zCoord)
        )

        return coord.xCoord >= min.xCoord && coord.xCoord <= max.xCoord &&
                coord.yCoord >= min.yCoord && coord.yCoord <= max.yCoord &&
                coord.zCoord >= min.zCoord && coord.zCoord <= max.zCoord
    }


    /**
     * Calculates the distance between two points in a 3D space using Vec3.
     * @param vec1 The first point as a Vec3.
     * @param vec2 The second point as a Vec3.
     * @return The distance between the two points.
     */
    fun distanceIn3DWorld(vec1: Vec3, vec2: Vec3): Double {
        return vec1.distanceTo(vec2)
    }


    /**
     * Calculates the distance between two points in a 2D space (ignoring the Y coordinate) using Vec3.
     * @param vec1 The first point as a Vec3.
     * @param vec2 The second point as a Vec3.
     * @return The distance between the two points in 2D space.
     */
    fun distanceIn2DWorld(vec1: Vec3, vec2: Vec3): Double {
        val deltaX = vec1.xCoord - vec2.xCoord
        val deltaZ = vec1.zCoord - vec2.zCoord
        return sqrt(deltaX.pow(2) + deltaZ.pow(2))
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

    /**
     * Calculates the yaw and pitch angles required to look at a specific block position.
     *
     * @param blockPos The block position object containing the x, y, and z coordinates.
     * @param playerPos The player position object containing the x, y, and z coordinates. If not provided, the player's eye position will be used.
     *
     * @return A Pair containing the yaw and pitch angles in degrees. If the calculation fails, returns null.
     */
    fun calcYawPitch(blockPos: Vec3, playerPos: Vec3 = getEyePos()): Rotation {
        val dx = blockPos.xCoord - playerPos.xCoord
        val dy = blockPos.yCoord - playerPos.yCoord
        val dz = blockPos.zCoord - playerPos.zCoord

        val yaw = if (dx != 0.0) (- (if (dx < 0) 1.5 * PI else 0.5 * PI - atan(dz / dx)) * 180 / PI).toFloat()
        else if (dz < 0) 180f
        else 0f

        val xzDistance = sqrt(dx.pow(2) + dz.pow(2))
        val pitch = (- (atan(dy / xzDistance) * 180 / PI)).toFloat()
        
        return Rotation(yaw, pitch)
    }

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

    fun Ease(t: Double): Double = sin((t * Math.PI) / 2)

    data class Rotation(val yaw: Float, val pitch: Float)
}