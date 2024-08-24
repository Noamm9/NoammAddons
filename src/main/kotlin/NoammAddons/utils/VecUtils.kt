package NoammAddons.utils

import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

object VecUtils {
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
}

data class Rotation(val pitch: Float, val yaw: Float)
