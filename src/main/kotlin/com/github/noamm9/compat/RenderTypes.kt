package com.github.noamm9.compat

import net.minecraft.client.renderer.rendertype.RenderSetup

enum class OitMode {
    FILLED, LINES, FILLED_THROUGH_WALLS, CIRCLE_FILLED_THROUGH_WALLS, LINES_THROUGH_WALLS;
}

//? if >=26.3 {
private object OitPipelines {
    val FILLED_THROUGH_WALLS = net.minecraft.client.renderer.RenderPipelines.register(
        net.minecraft.client.renderer.oit.OitPipelineSet.builder(
            "noammaddons_filled_through_walls",
            RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.OIT_DEBUG_FILLED_SNIPPET).apply {
                withVertexBinding(0, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR)
                withPrimitiveTopology(PrimitiveTopology.QUADS)
                withCull(true)
            }
        ).withoutDepthTest().build()
    )

    val CIRCLE_FILLED_THROUGH_WALLS = net.minecraft.client.renderer.RenderPipelines.register(
        net.minecraft.client.renderer.oit.OitPipelineSet.builder(
            "noammaddons_circle_filled_through_walls",
            RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.OIT_DEBUG_FILLED_SNIPPET).apply {
                withVertexBinding(0, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR)
                withPrimitiveTopology(PrimitiveTopology.QUADS)
                withCull(true)
            }
        ).withoutDepthTest().build()
    )

    val LINES_THROUGH_WALLS = net.minecraft.client.renderer.RenderPipelines.register(
        net.minecraft.client.renderer.oit.OitPipelineSet.builder(
            "noammaddons_lines_through_walls",
            RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.OIT_LINES_SNIPPET)
        ).withoutDepthTest().build()
    )
}
//? }

fun RenderSetup.RenderSetupBuilder.withOit(mode: OitMode): RenderSetup.RenderSetupBuilder {
    //? if <26.3 {
    /*// 26.2 has no OIT pipeline sets, so the render types use the plain pipelines only and
    // MixinSubmitNodeCollection routes them into phases drawn after translucent terrain.
    return this
    *///? } else {
    return setOitPipelines(
        when (mode) {
            OitMode.FILLED -> net.minecraft.client.renderer.RenderPipelines.OIT_DEBUG_FILLED_BOX
            OitMode.LINES -> net.minecraft.client.renderer.RenderPipelines.OIT_LINES_TRANSLUCENT
            OitMode.FILLED_THROUGH_WALLS -> OitPipelines.FILLED_THROUGH_WALLS
            OitMode.CIRCLE_FILLED_THROUGH_WALLS -> OitPipelines.CIRCLE_FILLED_THROUGH_WALLS
            OitMode.LINES_THROUGH_WALLS -> OitPipelines.LINES_THROUGH_WALLS
        }
    )
    //? }
}
