package com.github.noamm9.utils.render.world.batches

import com.github.noamm9.utils.render.world.RenderBatcher.tmpVec
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline

data class FilledBatch(val pipeline: URenderPipeline, val mode: UGraphics.DrawMode) {
    @JvmRecord
    data class FilledRenderState(
        val x: Double, val y: Double, val z: Double,
        val r: Float, val g: Float, val b: Float, val a: Float,
    )

    val data = ArrayList<FilledRenderState>()

    fun addFilledBoxVertices(pose: UMatrixStack, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, r: Float, g: Float, b: Float, a: Float) {
        val minX = x1.toFloat() - 0.002f
        val minY = y1.toFloat() - 0.002f
        val minZ = z1.toFloat() - 0.002f
        val maxX = x2.toFloat() + 0.002f
        val maxY = y2.toFloat() + 0.002f
        val maxZ = z2.toFloat() + 0.002f

        addQuad(pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a)
        addQuad(pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a)
        addQuad(pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a)
        addQuad(pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a)
        addQuad(pose, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a)
        addQuad(pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a)
    }

    fun vertex(pose: UMatrixStack, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float) {
        pose.peek().model.transformPosition(x, y, z, tmpVec)
        data.add(FilledRenderState(
            tmpVec.x.toDouble(), tmpVec.y.toDouble(), tmpVec.z.toDouble(),
            r, g, b, a
        ))
    }

    fun addQuad(pose: UMatrixStack, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float, r: Float, g: Float, b: Float, a: Float) {
        vertex(pose, x1, y1, z1, r, g, b, a)
        vertex(pose, x2, y2, z2, r, g, b, a)
        vertex(pose, x3, y3, z3, r, g, b, a)
        vertex(pose, x1, y1, z1, r, g, b, a)
        vertex(pose, x3, y3, z3, r, g, b, a)
        vertex(pose, x4, y4, z4, r, g, b, a)
    }
}