package com.github.noamm9.utils.render.world

import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

/**
 * @see com.github.noamm9.mixin.MixinSubmitNodeCollection
 * submits these to post terrain phase.
 */
object NoammRenderLayers {
    val FILLED = RenderType.create("noamm_filled", RenderSetup.builder(NoammRenderPipelines.FILLED).createRenderSetup())
    val FILLED_THROUGH_WALLS = RenderType.create("noamm_filled_through_walls", RenderSetup.builder(NoammRenderPipelines.FILLED_THROUGH_WALLS).createRenderSetup())

    val CIRCLE_FILLED = RenderType.create("noamm_circle_filled", RenderSetup.builder(NoammRenderPipelines.CIRCLE_FILLED).createRenderSetup())
    val CIRCLE_FILLED_THROUGH_WALLS = RenderType.create("noamm_circle_filled_through_walls", RenderSetup.builder(NoammRenderPipelines.CIRCLE_FILLED_THROUGH_WALLS).createRenderSetup())

    val LINES = RenderType.create("noamm_lines", RenderSetup.builder(NoammRenderPipelines.LINES).createRenderSetup())
    val LINES_THROUGH_WALLS = RenderType.create("noamm_lines_through_walls", RenderSetup.builder(NoammRenderPipelines.LINES_THROUGH_WALLS).createRenderSetup())

    @JvmField val phaseLayers = setOf(FILLED_THROUGH_WALLS, CIRCLE_FILLED_THROUGH_WALLS, LINES_THROUGH_WALLS)
    @JvmField val afterTerrainLayers = setOf(FILLED, CIRCLE_FILLED, LINES)
}
