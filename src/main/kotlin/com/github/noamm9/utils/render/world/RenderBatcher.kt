package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.render.world.batches.FilledBatch
import com.github.noamm9.utils.render.world.batches.LineBatch
import com.github.noamm9.utils.render.world.batches.TextRenderState
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.universal.vertex.UBuiltBuffer
import gg.essential.universal.vertex.UVertexConsumer
import net.minecraft.client.gui.Font
import net.minecraft.util.LightCoordsUtil
import org.joml.Matrix4f
import org.joml.Vector3f

object RenderBatcher {
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

    internal fun flush() {
        if (filledBatches.isEmpty() && lineBatches.isEmpty() && texts.isEmpty()) return

        val pendingFills = filledBatches.values.toList().also { filledBatches.clear() }
        val pendingLines = lineBatches.values.toList().also { lineBatches.clear() }
        val pendingTexts = texts.toList().also { texts.clear() }

        if (pendingTexts.isNotEmpty()) {
            val consumers = mc.renderBuffers().bufferSource()
            for (text in pendingTexts) UMinecraft.getFontRenderer().drawInBatch(
                text.text,
                text.xOff,
                text.yOff,
                text.argb,
                true,
                text.matrix,
                consumers,
                if (text.seeThrough) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL,
                0,
                LightCoordsUtil.FULL_BRIGHT
            )
            consumers.endBatch()
        }

        for (batchData in pendingFills) {
            val builder = UBufferBuilder.create(batchData.mode, UGraphics.CommonVertexFormats.POSITION_COLOR)

            for (state in batchData.data) {
                builder.pos(UMatrixStack.UNIT, state.x, state.y, state.z)
                builder.color(state.r, state.g, state.b, state.a)
                builder.endVertex()
            }

            builder.build()?.drawAndClose(batchData.pipeline) { noScissor() }
        }

        for (batchData in pendingLines) {
            val mcBuffer = Tesselator.getInstance().begin(UGraphics.DrawMode.LINES.mcMode,
                DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
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
    }

    private fun filledBatch(pipeline: URenderPipeline, mode: UGraphics.DrawMode) = filledBatches.getOrPut(pipeline) { FilledBatch(pipeline, mode) }
}