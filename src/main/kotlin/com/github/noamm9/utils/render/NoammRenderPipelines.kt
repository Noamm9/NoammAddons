package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation

object NoammRenderPipelines {
    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id("lines_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val FILLED_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("filled_through_walls"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val ROUND_RECT: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(NoammAddons.MOD_ID, "pipeline/round_rect"))
            .withVertexShader(ResourceLocation.fromNamespaceAndPath(NoammAddons.MOD_ID, "core/round_rect"))
            .withFragmentShader(ResourceLocation.fromNamespaceAndPath(NoammAddons.MOD_ID, "core/round_rect"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withUniform("u", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
            .build()
    )

    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath(NoammAddons.MOD_ID, path)
}