package noammaddons.features.impl.general.teleport

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.MathUtils.floor
import kotlin.math.*

object InstantTransmissionPredictor {
    private val IGNORED = setOf(0, 51, 8, 9, 10, 11, 171, 331, 39, 40, 115, 132, 77, 143, 66, 27, 28, 157)
    private val IGNORED2 = setOf(44, 182, 126)
    private val SPECIAL = setOf(65, 106, 111)

    private const val STEPS = 100.0

    fun predictTeleport(distance: Double, x: Double, y: Double, z: Double, yaw: Double, pitch: Double): Vec3? {
        val forward = Vector3.fromPitchYaw(pitch, yaw).multiply(1.0 / STEPS)
        val cur = Vector3(x, y + EtherwarpHelper.getPlayerEyeHeight(), z)
        var i = 0.0

        while (i <= distance * STEPS) {
            if (i % STEPS == 0.0 && ! isSpecial(cur) && ! isSpecial(cur.addY(1.0))) {
                if (! isIgnored(cur) || ! isIgnored(cur.addY(1.0))) {
                    cur.add(forward.multiply(- STEPS))
                    return if (i == 0.0 || ! isIgnored(cur) || ! isIgnored(cur.addY(1.0))) {
                        null
                    }
                    else {
                        Vec3(floor(cur.x) + 0.5, floor(cur.y), floor(cur.z) + 0.5)
                    }
                }
            }

            if ((! isIgnored2(cur) && inBB(cur)) || (! isIgnored2(cur.addY(1.0)) && inBB(cur.addY(1.0)))) {
                cur.add(forward.multiply(- STEPS))
                return if (i == 0.0 || (! isIgnored(cur) && inBB(cur)) || (! isIgnored(cur.addY(1.0)) && inBB(cur.addY(1.0)))) {
                    null
                }
                else {
                    break
                }
            }

            cur.add(forward)
            i ++
        }

        val pos = Vector3(x, y + EtherwarpHelper.getPlayerEyeHeight(), z)
            .add(Vector3.fromPitchYaw(pitch, yaw).multiply(i / STEPS))

        return if ((! isIgnored(cur) && inBB(cur)) || (! isIgnored(cur.addY(1.0)) && inBB(cur.addY(1.0)))) {
            null
        }
        else {
            Vec3(floor(pos.x) + 0.5, floor(pos.y), floor(pos.z) + 0.5)
        }
    }

    private fun isIgnored(vec: Vector3): Boolean {
        val blockId = getBlockAt(Vec3(vec.x, vec.y, vec.z).floor()).getBlockId()
        return IGNORED.contains(blockId)
    }

    private fun isIgnored2(vec: Vector3): Boolean {
        val blockId = getBlockAt(Vec3(vec.x, vec.y, vec.z).floor()).getBlockId()
        return isIgnored(vec) || IGNORED2.contains(blockId)
    }

    private fun isSpecial(vec: Vector3): Boolean {
        val blockId = getBlockAt(Vec3(vec.x, vec.y, vec.z).floor()).getBlockId()
        return SPECIAL.contains(blockId)
    }

    private fun inBB(vec: Vector3): Boolean {
        val pos = Vec3(vec.x, vec.y, vec.z).floor()
        val boundingBox = getBlockAt(pos).getSelectedBoundingBox(mc.theWorld, BlockPos(pos))
        return boundingBox.isVecInside(pos)
    }

    private data class Vector3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
        companion object {
            fun fromPitchYaw(pitch: Double, yaw: Double): Vector3 {
                val f = cos(- yaw * 0.017453292 - Math.PI)
                val f1 = sin(- yaw * 0.017453292 - Math.PI)
                val f2 = - cos(- pitch * 0.017453292)
                val f3 = sin(- pitch * 0.017453292)
                return Vector3(f1 * f2, f3, f * f2).normalize()
            }
        }

        fun getLength() = sqrt(x * x + y * y + z * z)

        fun add(vector3: Vector3): Vector3 {
            x += vector3.x
            y += vector3.y
            z += vector3.z
            return this
        }

        fun addY(value: Double): Vector3 {
            return Vector3(x, y + value, z)
        }

        fun normalize(): Vector3 {
            val len = getLength()
            if (len != 0.0) {
                x /= len
                y /= len
                z /= len
            }
            return this
        }

        fun multiply(factor: Double): Vector3 {
            x *= factor
            y *= factor
            z *= factor
            return this
        }
    }
}

