package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.render.world.iris.IrisCompatibility
import com.github.noamm9.utils.render.world.iris.IrisShaderType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.*
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.oit.OitPipelineSet
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import java.util.*

object NoammRenderTypes: ISelfInit {
    private val MC_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withCull(true)
        }.build()
    )

    private val MC_FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled_through_walls"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withCull(true)
            withDepthStencilState(Optional.empty())
        }.build()
    )

    private val MC_CIRCLE_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withCull(true)
        }.build()
    )

    private val MC_CIRCLE_FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled_through_walls"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withCull(true)
            withDepthStencilState(Optional.empty())
        }.build()
    )

    private val MC_LINES = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).apply {
            withLocation(id("pipeline/lines"))
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        }.build()
    )

    private val MC_LINES_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).apply {
            withLocation(id("pipeline/lines_through_walls"))
            withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            withDepthStencilState(Optional.empty())
        }.build()
    )

    private val OIT_FILLED_THROUGH_WALLS = RenderPipelines.register(
        OitPipelineSet.builder(
            "noammaddons_filled_through_walls",
            RenderPipeline.builder(RenderPipelines.OIT_DEBUG_FILLED_SNIPPET).apply {
                withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                withCull(true)
            }
        ).withoutDepthTest().build()
    )

    private val OIT_CIRCLE_FILLED_THROUGH_WALLS = RenderPipelines.register(
        OitPipelineSet.builder(
            "noammaddons_circle_filled_through_walls",
            RenderPipeline.builder(RenderPipelines.OIT_DEBUG_FILLED_SNIPPET).apply {
                withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
                withCull(true)
            }
        ).withoutDepthTest().build()
    )

    private val OIT_LINES_THROUGH_WALLS = RenderPipelines.register(
        OitPipelineSet.builder(
            "noammaddons_lines_through_walls",
            RenderPipeline.builder(RenderPipelines.OIT_LINES_SNIPPET)
        ).withoutDepthTest().build()
    )

    val DEBUG_CIRCLE_FILLED = RenderType.create("NOAMM_DEBUG_CIRCLE_FILLED", RenderSetup.builder(MC_CIRCLE_FILLED_THROUGH_WALLS).setOitPipelines(OIT_CIRCLE_FILLED_THROUGH_WALLS).sortOnUpload().createRenderSetup())
    val DEBUG_FILLED = RenderType.create("NOAMM_DEBUG_FILLED", RenderSetup.builder(MC_FILLED_THROUGH_WALLS).setOitPipelines(OIT_FILLED_THROUGH_WALLS).sortOnUpload().createRenderSetup())
    val DEBUG_LINES = RenderType.create("NOAMM_DEBUG_LINES", RenderSetup.builder(MC_LINES_THROUGH_WALLS).setOitPipelines(OIT_LINES_THROUGH_WALLS).sortOnUpload().createRenderSetup())

    val CIRCLE_FILLED = RenderType.create("NOAMM_CIRCLE_FILLED", RenderSetup.builder(MC_CIRCLE_FILLED).setOitPipelines(RenderPipelines.OIT_DEBUG_FILLED_BOX).sortOnUpload().createRenderSetup())
    val FILLED = RenderType.create("NOAMM_FILLED", RenderSetup.builder(MC_FILLED).setOitPipelines(RenderPipelines.OIT_DEBUG_FILLED_BOX).sortOnUpload().createRenderSetup())
    val LINES = RenderType.create("NOAMM_LINES", RenderSetup.builder(MC_LINES).setOitPipelines(RenderPipelines.OIT_LINES_TRANSLUCENT).sortOnUpload().createRenderSetup())

    override fun init() {
        IrisCompatibility.registerPipeline(MC_FILLED, IrisShaderType.LINES)
        IrisCompatibility.registerPipeline(MC_FILLED_THROUGH_WALLS, IrisShaderType.BASIC)

        IrisCompatibility.registerPipeline(MC_CIRCLE_FILLED, IrisShaderType.BASIC)
        IrisCompatibility.registerPipeline(MC_CIRCLE_FILLED_THROUGH_WALLS, IrisShaderType.BASIC)

        IrisCompatibility.registerPipeline(MC_LINES, IrisShaderType.LINES)
        IrisCompatibility.registerPipeline(MC_LINES_THROUGH_WALLS, IrisShaderType.LINES)
    }

    private fun id(path: String) = Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, path)
}