package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.NumbersUtils.div
import com.github.noamm9.utils.render.RenderHelper.renderVec
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.*
import kotlin.random.Random

object MathUtils {
    data class Rotation(var yaw: Float, var pitch: Float)

    fun isCoordinateInsideBox(coord: Vec3, corner1: Vec3i, corner2: Vec3i): Boolean {
        val minX = minOf(corner1.x, corner2.x)
        val maxX = maxOf(corner1.x, corner2.x)
        val minY = minOf(corner1.y, corner2.y)
        val maxY = maxOf(corner1.y, corner2.y)
        val minZ = minOf(corner1.z, corner2.z)
        val maxZ = maxOf(corner1.z, corner2.z)

        val x = coord.x.toInt() in minX .. maxX
        val y = coord.y.toInt() in minY .. maxY
        val z = coord.z.toInt() in minZ .. maxZ
        return x && y && z
    }

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

    fun gaussianRandom(min: Int, max: Int): Int {
        val u1 = 1.0 - Random.nextDouble()
        val u2 = 1.0 - Random.nextDouble()
        val gaussian = sqrt(- 2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)

        val mean = min + (max - min) / 2.0
        val stdDev = (max - min) / 6.0

        val result = (gaussian * stdDev) + mean

        return max(min.toDouble(), min(max.toDouble(), result)).toInt()
    }

    fun distance2D(vec1: Vec3, vec2: Vec3): Double {
        val deltaX = vec1.x - vec2.x
        val deltaZ = vec1.z - vec2.z
        return sqrt(deltaX * deltaX + deltaZ * deltaZ)
    }

    fun normalizeYaw(value: Float): Float {
        var value = value
        value %= 360.0f
        if (value >= 180.0f) value -= 360.0f
        if (value < - 180.0f) value += 360.0f

        return value
    }

    fun normalizePitch(num: Float): Float {
        return if (num < - 90f) - 90f
        else if (num > 90f) 90f else num
    }

    fun fixRot(rot: Rotation, lastRot: Rotation): Rotation {
        val yaw = rot.yaw
        val pitch = rot.pitch

        val lastYaw = lastRot.yaw
        val lastPitch = lastRot.pitch

        val f = mc.options.sensitivity().get().toFloat() * 0.6f + 0.2f
        val gcd = f * f * f * 1.2f

        val dYaw = yaw - lastYaw
        val dPitch = pitch - lastPitch

        val fixedDYaw = dYaw - (dYaw % gcd)
        val fixedDPitch = dPitch - (dPitch % gcd)

        val fixedYaw = lastYaw + fixedDYaw
        val fixedPitch = lastPitch + fixedDPitch

        return Rotation(fixedYaw, fixedPitch)
    }

    fun calcYawPitch(blockPos: Vec3, playerPos: Vec3 = mc.player !!.renderVec.add(y = mc.player !!.eyeHeight)): Rotation {
        val delta = blockPos.subtract(playerPos)
        val yaw = - atan2(delta.x, delta.z) * (180 / PI)
        val pitch = - atan2(delta.y, sqrt(delta.x * delta.x + delta.z * delta.z)) * (180 / PI)
        return Rotation(yaw.toFloat(), pitch.toFloat())
    }

    @JvmStatic
    fun lerp(prev: Number, newPos: Number, partialTicks: Number): Double {
        return prev.toDouble() + (newPos.toDouble() - prev.toDouble()) * partialTicks.toDouble()
    }

    fun lerpColor(color1: Color, color2: Color, value: Number): Color {
        return Color(
            lerp(color1.red, color2.red, value).toInt(),
            lerp(color1.green, color2.green, value).toInt(),
            lerp(color1.blue, color2.blue, value).toInt()
        )
    }

    fun interpolateYaw(startYaw: Float, targetYaw: Float, progress: Float): Float {
        var delta = (targetYaw - startYaw) % 360

        if (delta > 180) delta -= 360
        if (delta < - 180) delta += 360

        return (startYaw + delta * progress)
    }

    fun BlockPos.add(x: Number = 0, y: Number = 0, z: Number = 0) = this.offset(x.toInt(), y.toInt(), z.toInt())
    fun BlockPos.toVec() = Vec3(x, y, z)

    fun Vec3.floor() = Vec3(floor(x), floor(y), floor(z))
    fun Vec3.toPos() = BlockPos(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())
    fun Vec3.add(x: Number = 0.0, y: Number = 0.0, z: Number = 0.0) = add(Vec3(x, y, z))
    fun Vec3i.destructured() = Triple(x, y, z)
    fun Vec3.destructured() = Triple(x, y, z)
    fun Vec3.copy() = Vec3(x, y, z)
    fun Vec3.center() = add(Vec3(0.5, 0.5, 0.5))
    fun Vec3.multiply(factor: Double) = Vec3(x * factor, y * factor, z * factor)
    fun Vec3.inAABB(aabb: AABB) = x in aabb.minX .. aabb.maxX && y in aabb.minY .. aabb.maxY && z in aabb.minZ .. aabb.maxZ
    fun Vec3.xzInAABB(aabb: AABB) = x in aabb.minX .. aabb.maxX && z in aabb.minZ .. aabb.maxZ

    @JvmName("Vec3")
    fun Vec3(x: Number, y: Number, z: Number): Vec3 = net.minecraft.world.phys.Vec3(x.toDouble(), y.toDouble(), z.toDouble())

    fun raytrace(player: LocalPlayer, range: Number): BlockPos? {
        var startVec = player.getEyePosition(1f)
        val lookVec = player.getViewVector(1f).normalize()

        val stepSize = 0.1
        val steps = (range / stepSize).toInt()

        repeat(steps) {
            startVec = startVec.add(lookVec.scale(stepSize))
            val pos = BlockPos.containing(startVec)
            if (WorldUtils.getStateAt(pos).isAir) return@repeat
            return pos
        }

        return null
    }

    fun getLookVec(yaw: Float, pitch: Float): Vec3 {
        val f = pitch * (Math.PI / 180.0).toFloat()
        val g = - yaw * (Math.PI / 180.0).toFloat()
        val h = Mth.cos(g)
        val i = Mth.sin(g)
        val j = Mth.cos(f)
        val k = Mth.sin(f)
        return Vec3((i * j).toDouble(), - k.toDouble(), (h * j).toDouble())
    }
}