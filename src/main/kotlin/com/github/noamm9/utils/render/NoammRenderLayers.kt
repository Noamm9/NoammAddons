package com.github.noamm9.utils.render

import it.unimi.dsi.fastutil.doubles.Double2ObjectMap
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.RenderType.CompositeState
import java.util.*
import java.util.function.DoubleFunction

object NoammRenderLayers {
    private val linesThroughWallsLayers: Double2ObjectMap<RenderType.CompositeRenderType> = Double2ObjectOpenHashMap()
    private val linesLayers: Double2ObjectMap<RenderType.CompositeRenderType> = Double2ObjectOpenHashMap()

    private val LINES_THROUGH_WALLS = DoubleFunction { width ->
        RenderType.create(
            "lines_through_walls",
            RenderType.TRANSIENT_BUFFER_SIZE, false, false,
            NoammRenderPipelines.LINES_THROUGH_WALLS,
            CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(width)))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false)
        )
    }

    private val LINES = DoubleFunction { width ->
        RenderType.create(
            "lines",
            RenderType.TRANSIENT_BUFFER_SIZE, false, false,
            RenderPipelines.LINES,
            CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(width)))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false)
        )
    }

    val FILLED: RenderType.CompositeRenderType = RenderType.create(
        "filled", RenderType.TRANSIENT_BUFFER_SIZE, false, true,
        RenderPipelines.DEBUG_FILLED_BOX,
        CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    )

    val FILLED_THROUGH_WALLS: RenderType.CompositeRenderType = RenderType.create(
        "filled_through_walls", RenderType.TRANSIENT_BUFFER_SIZE, false, true,
        NoammRenderPipelines.FILLED_THROUGH_WALLS,
        CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    )

    fun getLinesThroughWalls(width: Double): RenderType.CompositeRenderType =
        linesThroughWallsLayers.computeIfAbsent(width, LINES_THROUGH_WALLS)

    fun getLines(width: Double): RenderType.CompositeRenderType =
        linesLayers.computeIfAbsent(width, LINES)
}
