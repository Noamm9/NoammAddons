package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.render.world.iris.IrisCompatibility
import com.github.noamm9.utils.render.world.iris.IrisShaderType
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import gg.essential.universal.render.URenderPipeline
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.util.*

object NoammRenderPipelines: ISelfInit {
    private val MC_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        }.build()
    )

    private val MC_FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled_through_walls"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            withDepthStencilState(Optional.empty())
        }.build()
    )

    private val MC_CIRCLE_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
        }.build()
    )

    private val MC_CIRCLE_FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled_through_walls"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            withDepthStencilState(Optional.empty())
        }.build()
    )

    private val MC_LINES = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).apply {
            withLocation(id("pipeline/lines"))
        }.build()
    )

    private val MC_LINES_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).apply {
            withLocation(id("pipeline/lines_through_walls"))
            withDepthStencilState(Optional.empty())
        }.build()
    )

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