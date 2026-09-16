package com.github.noamm9.utils.render.world

import com.github.noamm9.utils.render.world.batches.*
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.network.chat.Component
import gg.essential.universal.*
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.vertex.*
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.gui.Font
import net.minecraft.util.LightCoordsUtil
import org.joml.Matrix4f
import org.joml.Vector3f

object RenderBatcher {
    private val lineBuffer = ByteBufferBuilder(64 * 1024)
    private val filledBatches = mutableMapOf<URenderPipeline, FilledBatch>()
    private val lineBatches = mutableMapOf<URenderPipeline, LineBatch>()
    private val texts = ArrayList<TextRenderState>()

    val tmpVec = Vector3f()
    val tmpDir = Vector3f()

    fun filledBatch(phase: Boolean) = filledBatch(if (phase) NoammRenderPipelines.FILLED_THROUGH_WALLS else NoammRenderPipelines.FILLED, UGraphics.DrawMode.TRIANGLES)
    fun circleBatch(phase: Boolean) = filledBatch(if (phase) NoammRenderPipelines.CIRCLE_FILLED_THROUGH_WALLS else NoammRenderPipelines.CIRCLE_FILLED, UGraphics.DrawMode.TRIANGLE_STRIP)
    fun lineBatch(phase: Boolean): LineBatch {
        val pipeline = if (phase) NoammRenderPipelines.LINES_THROUGH_WALLS else NoammRenderPipelines.LINES
        return lineBatches.getOrPut(pipeline) { LineBatch(pipeline) }
    }

    internal fun addText(matrix: Matrix4f, text: String, xOff: Float, yOff: Float, argb: Int, seeThrough: Boolean) {
        texts.add(TextRenderState(Matrix4f(matrix), text, xOff, yOff, argb, seeThrough))
    }

    internal fun submitText(context: LevelRenderContext) {
        for (text in texts) {
            val pose = PoseStack()
            pose.mulPose(text.matrix)
            context.submitNodeCollector().submitText(
                pose, text.xOff, text.yOff, Component.literal(text.text).visualOrderText, true,
                if (text.seeThrough) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT, text.argb, 0, 0
            )
        }
        texts.clear()
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
        texts.clear()
    }

    internal fun close() = lineBuffer.close()

    private fun filledBatch(pipeline: URenderPipeline, mode: UGraphics.DrawMode) = filledBatches.getOrPut(pipeline) { FilledBatch(pipeline, mode) }
}