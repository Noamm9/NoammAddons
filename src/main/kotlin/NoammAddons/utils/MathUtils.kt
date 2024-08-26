package NoammAddons.utils

import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

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
            Math.min(corner1.xCoord, corner2.xCoord),
            Math.min(corner1.yCoord, corner2.yCoord),
            Math.min(corner1.zCoord, corner2.zCoord)
        )

        val max = Vec3(
            Math.max(corner1.xCoord, corner2.xCoord),
            Math.max(corner1.yCoord, corner2.yCoord),
            Math.max(corner1.zCoord, corner2.zCoord)
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
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }

    fun getRotation(from: Vec3, to: Vec3): Rotation {
        val vec3 = to.subtract(from)
        return Rotation(
            -Math.toDegrees(atan2(vec3.yCoord, sqrt(vec3.xCoord * vec3.xCoord + vec3.zCoord * vec3.zCoord))).toFloat(),
            -Math.toDegrees(atan2(vec3.xCoord, vec3.zCoord)).toFloat()
        )
    }

    fun getVecFromRotation(rotation: Rotation): Vec3 {
        val f = MathHelper.cos(-rotation.yaw * 0.017453292f - Math.PI.toFloat())
        val f1 = MathHelper.sin(-rotation.yaw * 0.017453292f - Math.PI.toFloat())
        val f2 = -MathHelper.cos(-rotation.pitch * 0.017453292f)
        val f3 = MathHelper.sin(-rotation.pitch * 0.017453292f)
        return Vec3((f1 * f2).toDouble(), f3.toDouble(), (f * f2).toDouble())
    }

    data class Rotation(val pitch: Float, val yaw: Float)

}