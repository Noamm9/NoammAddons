package noammaddons.utils

import net.minecraft.util.Vec3

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

    fun Double.toFixed(decimals: Int): String {
        require(decimals >= 0) { "Decimal places must be non-negative" }
        return "%.${decimals}f".format(this)
    }

    fun Float.toFixed(decimals: Int): String {
        require(decimals >= 0) { "Decimal places must be non-negative" }
        return "%.${decimals}f".format(this.toDouble())
    }

    fun String.toFixed(decimals: Int): String {
        require(decimals >= 0) { "Decimal places must be non-negative" }
        val number = this.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid string format")
        return "%.${decimals}f".format(number)
    }

    data class Rotation(val yaw: Float, val pitch: Float)
}