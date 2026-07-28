package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.render.iris.IrisCompatibility
import com.github.noamm9.utils.render.iris.IrisShaderType
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.util.*

object NoammRenderPipelines: ISelfInit {
    val FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        }.build()
    )

    val CIRCLE_FILLED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
        }.build()
    )

    val LINES_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).apply {
            withLocation(id("pipeline/lines_through_walls"))
            withDepthStencilState(Optional.empty())
        }.build()
    )

    val LINES = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).apply {
            withLocation(id("pipeline/lines"))
        }.build()
    )

    val FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/filled_through_walls"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            withDepthStencilState(Optional.empty())
        }.build()
    )

    val CIRCLE_FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET).apply {
            withLocation(id("pipeline/circle_filled_through_walls"))
            withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            withDepthStencilState(Optional.empty())
        }.build()
    )

    override fun init() {
        IrisCompatibility.registerPipeline(LINES_THROUGH_WALLS, IrisShaderType.LINES)
        IrisCompatibility.registerPipeline(FILLED_THROUGH_WALLS, IrisShaderType.BASIC)
        IrisCompatibility.registerPipeline(CIRCLE_FILLED_THROUGH_WALLS, IrisShaderType.BASIC)
    }

    private fun id(path: String) = Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, path)
}