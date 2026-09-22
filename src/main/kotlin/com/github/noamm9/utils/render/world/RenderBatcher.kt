package com.github.noamm9.utils.render.world

import com.github.noamm9.utils.render.world.batches.FilledBatch
import com.github.noamm9.utils.render.world.batches.LineBatch
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import org.joml.Vector3f

object RenderBatcher {
    private val filledBatches = mutableMapOf<RenderType, FilledBatch>()
    private val lineBatches = mutableMapOf<RenderType, LineBatch>()
    private val poseStack = PoseStack()

    val tmpVec = Vector3f()
    val tmpDir = Vector3f()

    fun filledBatch(phase: Boolean): FilledBatch {
        val type = if (phase) NoammRenderTypes.DEBUG_FILLED else NoammRenderTypes.FILLED
        return filledBatches.getOrPut(type) { FilledBatch(type) }
    }

    fun circleBatch(phase: Boolean): FilledBatch {
        val type = if (phase) NoammRenderTypes.DEBUG_CIRCLE_FILLED else NoammRenderTypes.CIRCLE_FILLED
        return filledBatches.getOrPut(type) { FilledBatch(type) }
    }

    fun lineBatch(phase: Boolean): LineBatch {
        val type = if (phase) NoammRenderTypes.DEBUG_LINES else NoammRenderTypes.LINES
        return lineBatches.getOrPut(type) { LineBatch(type) }
    }

    fun flush(collector: SubmitNodeCollector) {
        if (filledBatches.isEmpty() && lineBatches.isEmpty()) return

        for (batchData in filledBatches.values) collector.submitCustomGeometry(poseStack, batchData.type) { _, consumer ->
            for (state in batchData.data) {
                consumer.addVertex(state.x, state.y, state.z)
                consumer.setColor(state.r, state.g, state.b, state.a)
            }
        }

        for (batchData in lineBatches.values) collector.submitCustomGeometry(poseStack, batchData.type) { _, consumer ->
            for (state in batchData.data) {
                consumer.addVertex(state.x, state.y, state.z)
                consumer.setColor(state.r, state.g, state.b, state.a)
                consumer.setNormal(state.nx, state.ny, state.nz)
                consumer.setLineWidth(state.lineWidth)
            }
        }

        filledBatches.clear()
        lineBatches.clear()
    }
}