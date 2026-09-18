package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.render.world.iris.IrisCompatibility
import com.github.noamm9.utils.render.world.iris.IrisShaderType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.renderpearl.api.pipeline.*
import gg.essential.universal.render.URenderPipeline
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import java.util.*

object NoammRenderPipelines: ISelfInit {
    private val MC_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        }.build()
    )

    private val MC_FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled_through_walls"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            withDepthStencilState(Optional.empty())
        }.build()
    )

    private val MC_CIRCLE_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
        }.build()
    )

    private val MC_CIRCLE_FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled_through_walls"))
            withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
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

    @Suppress("unused") // todo
    object Types {
        val DEBUG_CIRCLE_FILLED = RenderType.create("NOAMM_DEBUG_CIRCLE_FILLED", RenderSetup.builder(MC_CIRCLE_FILLED_THROUGH_WALLS).createRenderSetup())
        val DEBUG_FILLED = RenderType.create("NOAMM_DEBUG_FILLED", RenderSetup.builder(MC_FILLED_THROUGH_WALLS).createRenderSetup())
        val DEBUG_LINES = RenderType.create("NOAMM_DEBUG_LINES", RenderSetup.builder(MC_LINES_THROUGH_WALLS).createRenderSetup())

        val CIRCLE_FILLED = RenderType.create("NOAMM_CIRCLE_FILLED", RenderSetup.builder(MC_CIRCLE_FILLED).createRenderSetup())
        val FILLED = RenderType.create("NOAMM_FILLED", RenderSetup.builder(MC_FILLED).createRenderSetup())
        val LINES = RenderType.create("NOAMM_LINES", RenderSetup.builder(MC_LINES).createRenderSetup())
    }

    val FILLED = URenderPipeline.wrap(MC_FILLED)
    val FILLED_THROUGH_WALLS = URenderPipeline.wrap(MC_FILLED_THROUGH_WALLS)

    val CIRCLE_FILLED = URenderPipeline.wrap(MC_CIRCLE_FILLED)
    val CIRCLE_FILLED_THROUGH_WALLS = URenderPipeline.wrap(MC_CIRCLE_FILLED_THROUGH_WALLS)

    val LINES = URenderPipeline.wrap(MC_LINES)
    val LINES_THROUGH_WALLS = URenderPipeline.wrap(MC_LINES_THROUGH_WALLS)

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