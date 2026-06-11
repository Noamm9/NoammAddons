package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemStack

/**
 * A dedicated PIP renderer for the storage-overlay **page** items, with its own texture and content-based caching.
 *
 * The shared [ItemRenderer] re-renders its item models every frame (throttled to 60 FPS). For the storage overlay -
 * which shows hundreds of static items - that GPU work ([net.minecraft.client.renderer.command.RenderDispatcher])
 * is the dominant remaining cost. Here the texture is only re-rendered when the batch's **content changes**
 * (item models or their positions, i.e. scroll / data / page changes); on a static frame the cached texture is reused,
 * so item rendering drops to a single blit.
 *
 * Only the page items use this (a single submit per frame -> one texture -> no shared-texture aliasing). The player
 * inventory and carried item keep the shared [ItemRenderer] unchanged.
 *
 * Animated items (enchant glint, animated models) must keep re-rendering for their animation to play, which would
 * invalidate the whole batch's texture continuously - one enchanted item in view and the cache is useless. They are
 * split out into [AnimatedItemRenderer] instead, re-rendered at a low rate that the slow glint sweep doesn't notice,
 * while the static items keep blitting their cached texture.
 *
 * Player heads re-bake their render state in [ItemRenderer.prepareItem] until their async skin resolves, so the
 * state-identity part of the hash flips exactly when the custom texture is in (never staying stuck on Steve).
 */
class CachedItemRenderer(vertexConsumers: MultiBufferSource.BufferSource): PictureInPictureRenderer<CachedItemRenderer.State>(vertexConsumers) {
    private var lastHash = 0
    private var hasRendered = false

    override fun textureIsReadyToBlit(state: State) = hasRendered && state.hash == lastHash

    override fun getTextureLabel() = NoammAddons.MOD_ID + "_cached_items"
    override fun getTranslateY(height: Int, windowScaleFactor: Int) = height / 2f
    override fun getRenderStateClass() = State::class.java

    override fun renderToTexture(state: State, poseStack: PoseStack) {
        ItemRenderer.renderItems(state.items)
        lastHash = state.hash
        hasRendered = true
    }

    class State(
        private val width: Int,
        private val height: Int,
        private val scissor: ScreenRectangle?,
        private val bounds: ScreenRectangle?,
        val items: List<GuiItemRenderState>,
        val hash: Int,
    ): PictureInPictureRenderState {
        override fun x0() = 0
        override fun y0() = 0
        override fun x1() = width
        override fun y1() = height
        override fun scissorArea() = scissor
        override fun bounds() = bounds
        override fun scale() = 1f
    }

    companion object {
        private val batchList = mutableListOf<GuiItemRenderState>()
        private val animatedList = mutableListOf<GuiItemRenderState>()

        fun add(ctx: GuiGraphics, item: ItemStack, x: Int, y: Int) {
            val state = ItemRenderer.prepareItem(ctx, item, x, y) ?: return
            if (item.hasFoil() || state.itemStackRenderState().isAnimated) animatedList.add(state) else batchList.add(state)
        }

        fun flush(ctx: GuiGraphics) {
            if (batchList.isEmpty() && animatedList.isEmpty()) return

            val screenRect = ScreenRectangle(0, 0, ctx.guiWidth(), ctx.guiHeight()).transformMaxBounds(ctx.pose())
            val scissor = ctx.scissorStack.peek()
            val bounds = scissor?.intersection(screenRect) ?: screenRect

            if (batchList.isNotEmpty()) {
                // Content hash: resolved-model identity + position per item (+ pose scale), so the texture is only
                // re-rendered when the visible item set, its layout, the gui scale, or a head skin actually changes
                // (loading heads re-bake their state per frame, so their identity flips here until the skin is in).
                var hash = 1
                for (s in batchList) {
                    hash = hash * 31 + System.identityHashCode(s.itemStackRenderState())
                    hash = hash * 31 + s.x()
                    hash = hash * 31 + s.y()
                }
                val pose = batchList[0].pose()
                hash = hash * 31 + pose.m00().toRawBits()
                hash = hash * 31 + pose.m11().toRawBits()

                ctx.guiRenderState.submitPicturesInPictureState(State(ctx.guiWidth(), ctx.guiHeight(), scissor, bounds, batchList.toList(), hash))
                batchList.clear()
            }

            if (animatedList.isNotEmpty()) {
                ctx.guiRenderState.submitPicturesInPictureState(AnimatedItemRenderer.State(ctx.guiWidth(), ctx.guiHeight(), scissor, bounds, animatedList.toList()))
                animatedList.clear()
            }
        }
    }
}

/**
 * Companion layer of [CachedItemRenderer] for the **animated** page items (enchant glint, animated models): they
 * re-render in their own texture so the animation keeps playing, without ever invalidating the static batch's
 * cached texture. A separate state class means a separate texture - no aliasing between the two layers.
 *
 * In Skyblock storages most gear has glint, so this layer is often the *majority* of the items - re-rendering it at
 * full frame rate would cost about what the unsplit renderer did, plus the overhead of the second texture. The glint
 * sweep is a slow (~seconds-long) animation though, so [TARGET_FPS] is capped low: visually identical, and the
 * enchanted majority re-renders 3x less often than before while the static items stopped re-rendering entirely.
 */
class AnimatedItemRenderer(vertexConsumers: MultiBufferSource.BufferSource): PictureInPictureRenderer<AnimatedItemRenderer.State>(vertexConsumers) {
    private var lastRenderAtNanos = 0L

    override fun textureIsReadyToBlit(state: State) = System.nanoTime() - lastRenderAtNanos < FRAME_INTERVAL_NANOS
    override fun getTextureLabel() = NoammAddons.MOD_ID + "_animated_items"
    override fun getTranslateY(height: Int, windowScaleFactor: Int) = height / 2f
    override fun getRenderStateClass() = State::class.java

    override fun renderToTexture(state: State, poseStack: PoseStack) {
        ItemRenderer.renderItems(state.items)
        lastRenderAtNanos = System.nanoTime()
    }

    class State(
        private val width: Int,
        private val height: Int,
        private val scissor: ScreenRectangle?,
        private val bounds: ScreenRectangle?,
        val items: List<GuiItemRenderState>,
    ): PictureInPictureRenderState {
        override fun x0() = 0
        override fun y0() = 0
        override fun x1() = width
        override fun y1() = height
        override fun scissorArea() = scissor
        override fun bounds() = bounds
        override fun scale() = 1f
    }

    companion object {
        private const val TARGET_FPS = 20
        private const val FRAME_INTERVAL_NANOS = 1_000_000_000L / TARGET_FPS
    }
}
