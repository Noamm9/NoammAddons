package com.github.noamm9.utils.render.world.batches

import com.github.noamm9.utils.render.world.RenderBatcher.tmpDir
import com.github.noamm9.utils.render.world.RenderBatcher.tmpVec
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import kotlin.math.sqrt

class LineBatch(val pipeline: URenderPipeline) {

    @JvmRecord
    data class LineRenderState(
        val x: Double, val y: Double, val z: Double,
        val r: Float, val g: Float, val b: Float, val a: Float,
        val nx: Float, val ny: Float, val nz: Float, val lineWidth: Float
    )

    val data = ArrayList<LineRenderState>()

    fun addLineBoxVertices(pose: UMatrixStack, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float, lineWidth: Float) {
        val minX = x1.toFloat() - 0.002f
        val minY = y1.toFloat() - 0.002f
        val minZ = z1.toFloat() - 0.002f
        val maxX = x2.toFloat() + 0.002f
        val maxY = y2.toFloat() + 0.002f
        val maxZ = z2.toFloat() + 0.002f

        line(pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, lineWidth)
        line(pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, lineWidth)
        line(pose, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a, lineWidth)
        line(pose, minX, minY, maxZ, minX, minY, minZ, r, g, b, a, lineWidth)

        line(pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth)
        line(pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, lineWidth)
        line(pose, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth)
        line(pose, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a, lineWidth)

        line(pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, lineWidth)
        line(pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth)
        line(pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, lineWidth)
        line(pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth)
    }

    fun vertex(pose: UMatrixStack, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float, nx: Float, ny: Float, nz: Float, lineWidth: Float) {
        pose.peek().model.transformPosition(x, y, z, tmpVec)
        pose.peek().normal.transform(tmpDir.set(nx, ny, nz))

        data.add(LineRenderState(
            tmpVec.x.toDouble(), tmpVec.y.toDouble(), tmpVec.z.toDouble(),
            r, g, b, a,
            tmpDir.x, tmpDir.y, tmpDir.z,
            lineWidth
        ))
    }

    fun line(pose: UMatrixStack, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float, lineWidth: Float) {
        var dx = x2 - x1
        var dy = y2 - y1
        var dz = z2 - z1
        val lengthSquared = dx * dx + dy * dy + dz * dz
        if (lengthSquared > 0f) {
            val inv = 1f / sqrt(lengthSquared)
            dx *= inv
            dy *= inv
            dz *= inv
        }

        vertex(pose, x1, y1, z1, r, g, b, a, dx, dy, dz, lineWidth)
        vertex(pose, x2, y2, z2, r, g, b, a, dx, dy, dz, lineWidth)
    }
}