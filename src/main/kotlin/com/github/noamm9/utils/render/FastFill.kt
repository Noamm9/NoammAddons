package com.github.noamm9.utils.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc

/**
 * Collects many solid-colour rectangles and submits them as a single [GuiElementRenderState] instead
 * of one per `GuiGraphics.fill` call.
 *
 * Each `fill` allocates a `ColoredRectangleRenderState` and pushes it onto the GUI render-state list -
 * the measured hot path behind storage-overlay frame drops (the slot grids and per-item rarity
 * backgrounds add up to thousands of fills per frame). Since every batched rect shares the same
 * (constant during a panel's draw) pose, one render state can emit all of them as quads in a single
 * [GuiElementRenderState.buildVertices], collapsing thousands of render states into one.
 *
 * Rects are drawn in insertion order (painter's), so callers must add them in the same order they
 * would otherwise `fill`, then [flush] once while the intended pose and scissor are still active.
 */
object FastFill {
    // Flat ring of (x0, y0, x1, y1, argb) ints, reused across frames to avoid per-rect allocation.
    private var buf = IntArray(2048)
    private var size = 0

    fun add(x0: Int, y0: Int, x1: Int, y1: Int, argb: Int) {
        if (size + 5 > buf.size) buf = buf.copyOf(buf.size * 2)
        buf[size++] = x0; buf[size++] = y0; buf[size++] = x1; buf[size++] = y1; buf[size++] = argb
    }

    fun addBorder(x: Int, y: Int, w: Int, h: Int, thickness: Int, argb: Int) {
        add(x, y, x + w, y + thickness, argb)
        add(x, y + h - thickness, x + w, y + h, argb)
        add(x, y + thickness, x + thickness, y + h - thickness, argb)
        add(x + w - thickness, y + thickness, x + w, y + h - thickness, argb)
    }

    /** Submits everything collected so far as one render state, using the graphics' current pose and scissor. */
    fun flush(ctx: GuiGraphics) {
        if (size == 0) return
        val rects = buf.copyOf(size)
        size = 0
        val pose = Matrix3x2f(ctx.pose())
        val scissor = ctx.scissorStack.peek()

        var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE; var maxX = Int.MIN_VALUE; var maxY = Int.MIN_VALUE
        var i = 0
        while (i < rects.size) {
            if (rects[i] < minX) minX = rects[i]
            if (rects[i + 1] < minY) minY = rects[i + 1]
            if (rects[i + 2] > maxX) maxX = rects[i + 2]
            if (rects[i + 3] > maxY) maxY = rects[i + 3]
            i += 5
        }
        val local = ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformMaxBounds(pose)
        val bounds = scissor?.intersection(local) ?: local
        ctx.guiRenderState.submitGuiElement(BatchedRects(pose, rects, scissor, bounds))
    }

    private class BatchedRects(
        private val pose: Matrix3x2fc,
        private val rects: IntArray,
        private val scissor: ScreenRectangle?,
        private val bnds: ScreenRectangle?,
    ): GuiElementRenderState {
        override fun buildVertices(vc: VertexConsumer) {
            var i = 0
            while (i < rects.size) {
                val x0 = rects[i].toFloat(); val y0 = rects[i + 1].toFloat()
                val x1 = rects[i + 2].toFloat(); val y1 = rects[i + 3].toFloat(); val c = rects[i + 4]
                vc.addVertexWith2DPose(pose, x0, y0).setColor(c)
                vc.addVertexWith2DPose(pose, x0, y1).setColor(c)
                vc.addVertexWith2DPose(pose, x1, y1).setColor(c)
                vc.addVertexWith2DPose(pose, x1, y0).setColor(c)
                i += 5
            }
        }

        override fun pipeline(): RenderPipeline = RenderPipelines.GUI
        override fun textureSetup(): TextureSetup = TextureSetup.noTexture()
        override fun scissorArea(): ScreenRectangle? = scissor
        override fun bounds(): ScreenRectangle? = bnds
    }
}
