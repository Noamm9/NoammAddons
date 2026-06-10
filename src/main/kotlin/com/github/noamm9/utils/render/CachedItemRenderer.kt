package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.component.DataComponents
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
 * Two things still force a re-render so they look correct:
 * - **animated** items (enchant glint, animated models) re-render every frame, else the animation would freeze;
 * - **player heads** mix their current skin render-info into the hash, so the cache invalidates the exact frame an
 *   async custom skin finishes loading (default Steve head -> custom texture) instead of staying stuck on Steve.
 */
class CachedItemRenderer(vertexConsumers: MultiBufferSource.BufferSource): PictureInPictureRenderer<CachedItemRenderer.State>(vertexConsumers) {
    private var lastHash = 0
    private var hasRendered = false
    private var lastRenderAtNanos = 0L

    // Animated batches re-render at 60 FPS so the animation keeps playing; everything else reuses the cache until
    // its content hash (item models, positions, gui scale, and head skins) changes.
    override fun textureIsReadyToBlit(state: State) =
        if (state.animated) System.nanoTime() - lastRenderAtNanos < FRAME_INTERVAL_NANOS
        else hasRendered && state.hash == lastHash

    override fun getTextureLabel() = NoammAddons.MOD_ID + "_cached_items"
    override fun getTranslateY(height: Int, windowScaleFactor: Int) = height / 2f
    override fun getRenderStateClass() = State::class.java

    override fun renderToTexture(state: State, poseStack: PoseStack) {
        ItemRenderer.renderItems(state.items)
        lastHash = state.hash
        hasRendered = true
        lastRenderAtNanos = System.nanoTime()
    }

    class State(
        private val width: Int,
        private val height: Int,
        private val scissor: ScreenRectangle?,
        private val bounds: ScreenRectangle?,
        val items: List<GuiItemRenderState>,
        val hash: Int,
        val animated: Boolean,
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
        private const val TARGET_FPS = 60
        private const val FRAME_INTERVAL_NANOS = 1_000_000_000L / TARGET_FPS
        private val batchList = mutableListOf<GuiItemRenderState>()
        private var hasAnimated = false
        private var skinHash = 1

        fun add(ctx: GuiGraphics, item: ItemStack, x: Int, y: Int) {
            val state = ItemRenderer.prepareItem(ctx, item, x, y) ?: return
            batchList.add(state)
            if (item.hasFoil() || state.itemStackRenderState().isAnimated) hasAnimated = true

            // Player heads: getOrDefault triggers/keeps the async skin load alive (even on cached frames) and returns
            // a different RenderInfo once the skin is in, so mixing its identity invalidates the cache exactly then.
            val profile = item.get(DataComponents.PROFILE)
            if (profile != null) skinHash = skinHash * 31 + System.identityHashCode(mc.playerSkinRenderCache().getOrDefault(profile))
        }

        fun flush(ctx: GuiGraphics) {
            if (batchList.isEmpty()) {
                hasAnimated = false
                skinHash = 1
                return
            }

            // Content hash: resolved-model identity + position per item (+ pose scale + head skins), so the texture is
            // only re-rendered when the visible item set, its layout, the gui scale, or a head skin actually changes.
            var hash = 1
            for (s in batchList) {
                hash = hash * 31 + System.identityHashCode(s.itemStackRenderState())
                hash = hash * 31 + s.x()
                hash = hash * 31 + s.y()
            }
            val pose = batchList[0].pose()
            hash = hash * 31 + pose.m00().toRawBits()
            hash = hash * 31 + pose.m11().toRawBits()
            hash = hash * 31 + skinHash

            val screenRect = ScreenRectangle(0, 0, ctx.guiWidth(), ctx.guiHeight()).transformMaxBounds(ctx.pose())
            val scissor = ctx.scissorStack.peek()
            val bounds = scissor?.intersection(screenRect) ?: screenRect

            ctx.guiRenderState.submitPicturesInPictureState(State(ctx.guiWidth(), ctx.guiHeight(), scissor, bounds, batchList.toList(), hash, hasAnimated))
            batchList.clear()
            hasAnimated = false
            skinHash = 1
        }
    }
}
