package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import com.github.noamm9.utils.render.iris.IrisCompatibility
import com.github.noamm9.utils.render.iris.IrisShaderType

object NoammRenderPipelines {
    val FILLED: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/filled"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .build()
    )

    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id("pipeline/lines_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val FILLED_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/filled_through_walls"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    fun init() {
        IrisCompatibility.registerPipeline(LINES_THROUGH_WALLS, IrisShaderType.LINES)
        IrisCompatibility.registerPipeline(FILLED_THROUGH_WALLS, IrisShaderType.BASIC)
    }

    private fun id(path: String) = Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, path)
}