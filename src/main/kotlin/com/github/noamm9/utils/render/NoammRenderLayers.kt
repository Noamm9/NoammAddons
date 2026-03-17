package com.github.noamm9.utils.render


import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object NoammRenderLayers {
    val FILLED = RenderType.create("noamm_filled", RenderSetup.builder(NoammRenderPipelines.FILLED).createRenderSetup())
    val FILLED_THROUGH_WALLS = RenderType.create("noamm_filled_through_walls", RenderSetup.builder(NoammRenderPipelines.FILLED_THROUGH_WALLS).createRenderSetup())

    val LINES = RenderType.create("noamm_lines", RenderSetup.builder(RenderPipelines.LINES).createRenderSetup())
    val LINES_THROUGH_WALLS = RenderType.create("noamm_lines_through_walls", RenderSetup.builder(NoammRenderPipelines.LINES_THROUGH_WALLS).createRenderSetup())
}
