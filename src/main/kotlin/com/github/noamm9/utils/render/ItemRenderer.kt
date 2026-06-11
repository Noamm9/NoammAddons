package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.PlayerSkinRenderCache
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ResolvableProfile
import org.joml.Matrix3x2f
import org.joml.Matrix4f
import java.util.Optional
import java.util.concurrent.CompletableFuture

class ItemRenderer(vertexConsumers: MultiBufferSource.BufferSource): PictureInPictureRenderer<ItemRenderer.ItemState>(vertexConsumers) {
    override fun textureIsReadyToBlit(itemState: ItemState) = System.nanoTime() - lastRenderAtNanos < FRAME_INTERVAL_NANOS
    override fun getTextureLabel() = NoammAddons.MOD_ID + "_" + this.javaClass.simpleName
    override fun getTranslateY(height: Int, windowScaleFactor: Int) = height / 2f
    override fun getRenderStateClass() = ItemState::class.java
    private var lastRenderAtNanos = System.nanoTime()

    override fun renderToTexture(itemState: ItemState, poseStack: PoseStack) {
        renderItems(itemState.items)
        lastRenderAtNanos = System.nanoTime()
    }

    data class ItemState(
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
        private const val TARGET_FPS = 60
        private const val FRAME_INTERVAL_NANOS = 1_000_000_000L / TARGET_FPS
        private val batchList = mutableListOf<GuiItemRenderState>()

        /**
         * Caches the resolved item model per stack so [net.minecraft.client.renderer.item.ItemModelResolver.updateForTopItem]
         * (full model resolution) runs only when a slot's stack actually changes, instead of every frame for every visible
         * item - the hot path behind storage-overlay frame drops. Keyed by identity ([ItemStack] has no value equals/hashCode):
         * a replaced stack misses and re-resolves, and unreferenced stacks are evicted automatically. The GPU texture render
         * is already throttled to [TARGET_FPS] via [textureIsReadyToBlit]; this removes the remaining per-frame CPU cost.
         */
        private class Resolved(val state: TrackingItemStackRenderState, val label: String)
        private val resolveCache = java.util.WeakHashMap<ItemStack, Resolved>()

        /**
         * Player heads need their own cache: the model bake snapshots the head's skin RenderInfo
         * ([net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer.extractArgument] calls getOrDefault),
         * which is the default Steve until the async skin resolves, and the vanilla skin cache expires 5 minutes
         * after last access. Caching per stack would therefore either freeze a Steve bake in, or re-trigger a
         * (network) skin resolve for every new stack instance of an already-seen head - e.g. the live stacks the
         * server sends when a storage page is clicked. Instead the baked state is keyed by **profile equality**
         * (any new stack of a known head reuses the custom-skin state - Steve never shows again) and only cached
         * once the skin future is done; until then the head re-bakes each frame so the flip is picked up immediately.
         * The strongly-held future stays observable past the vanilla cache expiry.
         *
         * Profile hashCode/equals are record method-handle chains over the full PropertyMap - too hot to run per
         * head per frame, so resolved heads are also written through to [resolveCache]: the per-frame path is then
         * a pure identity lookup, and the profile is only hashed once per new stack instance.
         */
        private class HeadEntry(val future: CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>) {
            var plain: Resolved? = null
            var foil: Resolved? = null
        }

        private val headCache = HashMap<ResolvableProfile, HeadEntry>()

        private fun bake(item: ItemStack): Resolved {
            val state = TrackingItemStackRenderState()
            mc.itemModelResolver.updateForTopItem(state, item, ItemDisplayContext.GUI, mc.level, mc.player, 0)
            return Resolved(state, item.item.name.string)
        }

        private fun resolveHead(item: ItemStack, profile: ResolvableProfile): Resolved {
            val entry = headCache.getOrPut(profile) { HeadEntry(mc.playerSkinRenderCache().lookup(profile)) }
            val hasFoil = item.hasFoil()
            (if (hasFoil) entry.foil else entry.plain)?.let {
                resolveCache[item] = it
                return it
            }
            val resolved = bake(item)
            if (entry.future.isDone) {
                if (hasFoil) entry.foil = resolved else entry.plain = resolved
                resolveCache[item] = resolved
            }
            return resolved
        }

        // All items prepared within a pose share the same transform: snapshot it once and hand the same instance to
        // every render state (they never mutate it) instead of allocating a Matrix3x2f per item per frame. On a pose
        // change a fresh instance is made, so states already submitted keep the transform they were prepared with.
        private var sharedPose = Matrix3x2f()

        private fun poseOf(ctx: GuiGraphics): Matrix3x2f {
            val p = ctx.pose()
            val s = sharedPose
            if (s.m00() == p.m00() && s.m01() == p.m01() && s.m10() == p.m10() && s.m11() == p.m11() && s.m20() == p.m20() && s.m21() == p.m21()) return s
            return Matrix3x2f(p).also { sharedPose = it }
        }

        /** Resolves [item]'s model (cached) and builds a GUI render state at (x, y) using the current pose/scissor, or null if empty. */
        fun prepareItem(ctx: GuiGraphics, item: ItemStack, x: Int, y: Int): GuiItemRenderState? {
            if (item.isEmpty) return null
            val resolved = resolveCache[item] ?: run {
                val profile = item.get(DataComponents.PROFILE)
                if (profile != null) resolveHead(item, profile)
                else bake(item).also { resolveCache[item] = it }
            }
            return GuiItemRenderState(resolved.label, poseOf(ctx), resolved.state, x, y, ctx.scissorStack.peek())
        }

        fun drawBatchedItemStack(ctx: GuiGraphics, item: ItemStack, x: Int, y: Int) {
            batchList.add(prepareItem(ctx, item, x, y) ?: return)
        }

        /** Shared item-model render loop used by the PIP renderers - submits each item's model to the feature dispatcher. */
        fun renderItems(items: List<GuiItemRenderState>) {
            val dispatcher = mc.gameRenderer.featureRenderDispatcher
            val guiScale = mc.window.guiScale
            val guiPose = PoseStack()

            fun renderItem(item: GuiItemRenderState) {
                guiPose.pushPose()
                guiPose.last().pose().mul(Matrix4f(
                    item.pose().m00(), item.pose().m10(), 0f, 0f,
                    item.pose().m01(), item.pose().m11(), 0f, 0f,
                    0f, 0f, 1f, 0f,
                    item.pose().m20(), item.pose().m21(), 0f, 1f
                ))
                guiPose.translate((item.x() + 8.0) * guiScale, (item.y() + 8.0) * guiScale, 150.0)
                guiPose.scale(16f * guiScale, - 16f * guiScale, 16f * guiScale)
                item.itemStackRenderState().submit(guiPose, dispatcher.submitNodeStorage, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
                guiPose.popPose()
            }

            var hasBlocks = false
            for (i in items.indices) {
                val item = items[i]
                if (! item.itemStackRenderState().usesBlockLight()) continue
                if (! hasBlocks) {
                    mc.gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_3D)
                    hasBlocks = true
                }
                renderItem(item)
            }
            if (hasBlocks) dispatcher.renderAllFeatures()

            var hasItems = false
            for (i in items.indices) {
                val item = items[i]
                if (item.itemStackRenderState().usesBlockLight()) continue
                if (! hasItems) {
                    mc.gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_FLAT)
                    hasItems = true
                }
                renderItem(item)
            }
            if (hasItems) dispatcher.renderAllFeatures()
        }

        fun endItemRendererBatch(ctx: GuiGraphics) {
            if (batchList.isEmpty()) return

            val screenRect = ScreenRectangle(0, 0, ctx.guiWidth(), ctx.guiHeight()).transformMaxBounds(ctx.pose())
            val scissor = ctx.scissorStack.peek()
            val bounds = scissor?.intersection(screenRect) ?: screenRect

            ctx.guiRenderState.submitPicturesInPictureState(ItemState(ctx.guiWidth(), ctx.guiHeight(), scissor, bounds, batchList.toList()))
            batchList.clear()
        }
    }
}