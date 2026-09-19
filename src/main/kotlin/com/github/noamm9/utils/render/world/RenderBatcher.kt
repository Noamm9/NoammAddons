package com.github.noamm9.utils.render.world

import com.github.noamm9.utils.render.world.batches.FilledBatch
import com.github.noamm9.utils.render.world.batches.LineBatch
import com.mojang.blaze3d.vertex.*
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.vertex.*
import org.joml.Vector3f

object RenderBatcher {
    private val lineBuffer = ByteBufferBuilder(64 * 1024)
    private val filledBatches = mutableMapOf<URenderPipeline, FilledBatch>()
    private val lineBatches = mutableMapOf<URenderPipeline, LineBatch>()

    val tmpVec = Vector3f()
    val tmpDir = Vector3f()

    fun filledBatch(phase: Boolean) = filledBatch(if (phase) NoammRenderPipelines.FILLED_THROUGH_WALLS else NoammRenderPipelines.FILLED, UGraphics.DrawMode.TRIANGLES)
    fun circleBatch(phase: Boolean) = filledBatch(if (phase) NoammRenderPipelines.CIRCLE_FILLED_THROUGH_WALLS else NoammRenderPipelines.CIRCLE_FILLED, UGraphics.DrawMode.TRIANGLE_STRIP)
    fun lineBatch(phase: Boolean): LineBatch {
        val pipeline = if (phase) NoammRenderPipelines.LINES_THROUGH_WALLS else NoammRenderPipelines.LINES
        return lineBatches.getOrPut(pipeline) { LineBatch(pipeline) }
    }

    internal fun flush() {
        if (filledBatches.isEmpty() && lineBatches.isEmpty()) return

        for (batchData in filledBatches.values) {
            val builder = UBufferBuilder.create(batchData.mode, UGraphics.CommonVertexFormats.POSITION_COLOR)

            for (state in batchData.data) {
                builder.pos(UMatrixStack.UNIT, state.x, state.y, state.z)
                builder.color(state.r, state.g, state.b, state.a)
                builder.endVertex()
            }

            builder.build()?.drawAndClose(batchData.pipeline) { noScissor() }
        }

        for (batchData in lineBatches.values) {
            val mcBuffer = BufferBuilder(lineBuffer, UGraphics.DrawMode.LINES.mcMode, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
            val uc = UVertexConsumer.of(mcBuffer)

            for (state in batchData.data) {
                uc.pos(UMatrixStack.UNIT, state.x, state.y, state.z)
                uc.color(state.r, state.g, state.b, state.a)
                uc.norm(UMatrixStack.UNIT, state.nx, state.ny, state.nz)
                mcBuffer.setLineWidth(state.lineWidth)
                uc.endVertex()
            }

            mcBuffer.build()?.let(UBuiltBuffer::wrap)?.drawAndClose(batchData.pipeline) { noScissor() }
        }

        filledBatches.clear()
        lineBatches.clear()
    }

    internal fun close() = lineBuffer.close()

    private fun filledBatch(pipeline: URenderPipeline, mode: UGraphics.DrawMode) = filledBatches.getOrPut(pipeline) { FilledBatch(pipeline, mode) }
}