package com.github.noamm9.utils.render.world

import com.github.noamm9.utils.render.world.batches.FilledBatch
import com.github.noamm9.utils.render.world.batches.LineBatch
//? if <26.2 {
import com.github.noamm9.utils.render.world.batches.TextRenderState
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import gg.essential.universal.*
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.vertex.*
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.gui.Font
import net.minecraft.util.LightCoordsUtil
import org.joml.Matrix4f
 //? } else {
/*import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
*///? }
import org.joml.Vector3f

object RenderBatcher {
    //~ if >=26.2 'URenderPipeline' -> 'RenderType'
    private val filledBatches = mutableMapOf<URenderPipeline, FilledBatch>()

    //~ if >=26.2 'URenderPipeline' -> 'RenderType'
    private val lineBatches = mutableMapOf<URenderPipeline, LineBatch>()

    //? if <26.2
    private val texts = ArrayList<TextRenderState>()

    //? if >=26.2
    //private val poseStack = PoseStack()

    val tmpVec = Vector3f()
    val tmpDir = Vector3f()

    //? if <26.2 {
    fun filledBatch(phase: Boolean) = filledBatch(if (phase) NoammRenderPipelines.FILLED_THROUGH_WALLS else NoammRenderPipelines.FILLED, UGraphics.DrawMode.TRIANGLES)
    fun circleBatch(phase: Boolean) = filledBatch(if (phase) NoammRenderPipelines.CIRCLE_FILLED_THROUGH_WALLS else NoammRenderPipelines.CIRCLE_FILLED, UGraphics.DrawMode.TRIANGLE_STRIP)
    fun lineBatch(phase: Boolean): LineBatch {
        val pipeline = if (phase) NoammRenderPipelines.LINES_THROUGH_WALLS else NoammRenderPipelines.LINES
        return lineBatches.getOrPut(pipeline) { LineBatch(pipeline) }
    }

    internal fun addText(matrix: Matrix4f, text: String, xOff: Float, yOff: Float, argb: Int, seeThrough: Boolean) {
        texts.add(TextRenderState(Matrix4f(matrix), text, xOff, yOff, argb, seeThrough))
    }

    internal fun flush(context: LevelRenderContext) {
        if (filledBatches.isEmpty() && lineBatches.isEmpty() && texts.isEmpty()) return

        for (text in texts) UMinecraft.getFontRenderer().drawInBatch(
            text.text,
            text.xOff,
            text.yOff,
            text.argb,
            true,
            text.matrix,
            context.bufferSource(),
            if (text.seeThrough) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL,
            0,
            LightCoordsUtil.FULL_BRIGHT
        )

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
            val mcBuffer = Tesselator.getInstance().begin(UGraphics.DrawMode.LINES.mcMode, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
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

    private fun filledBatch(pipeline: URenderPipeline, mode: UGraphics.DrawMode) = filledBatches.getOrPut(pipeline) { FilledBatch(pipeline, mode) }
     //? } else {
    /*fun filledBatch(phase: Boolean): FilledBatch {
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
    *///? }
}