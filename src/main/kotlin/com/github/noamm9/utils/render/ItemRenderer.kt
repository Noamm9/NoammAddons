package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.renderer.state.gui.GuiItemRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import org.joml.Matrix4f

class ItemRenderer(vertexConsumers: MultiBufferSource.BufferSource): PictureInPictureRenderer<ItemRenderer.ItemState>(vertexConsumers) {
    override fun textureIsReadyToBlit(itemState: ItemState) = System.nanoTime() - lastRenderAtNanos < FRAME_INTERVAL_NANOS
    override fun getTextureLabel() = NoammAddons.MOD_ID + "_" + this.javaClass.simpleName
    override fun getTranslateY(height: Int, windowScaleFactor: Int) = height / 2f
    override fun getRenderStateClass() = ItemState::class.java
    private var lastRenderAtNanos = System.nanoTime()

    override fun renderToTexture(itemState: ItemState, poseStack: PoseStack) {
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
            item.itemStackRenderState().submit(guiPose, dispatcher.submitNodeStorage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
            guiPose.popPose()
        }

        var hasBlocks = false
        for (i in itemState.items.indices) {
            val item = itemState.items[i]
            if (! item.itemStackRenderState().usesBlockLight()) continue

            if (! hasBlocks) {
                mc.gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_3D)
                hasBlocks = true
            }

            renderItem(item)
        }

        if (hasBlocks) dispatcher.renderAllFeatures()

        var hasItems = false
        for (i in itemState.items.indices) {
            val item = itemState.items[i]
            if (item.itemStackRenderState().usesBlockLight()) continue

            if (! hasItems) {
                mc.gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_FLAT)
                hasItems = true
            }

            renderItem(item)
        }

        if (hasItems) dispatcher.renderAllFeatures()

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

        fun drawBatchedItemStack(ctx: GuiGraphicsExtractor, item: ItemStack, x: Int, y: Int) {
            if (item.isEmpty) return
            val tracking = TrackingItemStackRenderState()
            mc.itemModelResolver.updateForTopItem(tracking, item, ItemDisplayContext.GUI, mc.level, mc.player, 0)
            batchList.add(GuiItemRenderState(Matrix3x2f(ctx.pose()), tracking, x, y, ctx.scissorStack.peek()))
        }

        fun endItemRendererBatch(ctx: GuiGraphicsExtractor) {
            if (batchList.isEmpty()) return

            val screenRect = ScreenRectangle(0, 0, ctx.guiWidth(), ctx.guiHeight()).transformMaxBounds(ctx.pose())
            val scissor = ctx.scissorStack.peek()
            val bounds = scissor?.intersection(screenRect) ?: screenRect

            ctx.guiRenderState.addPicturesInPictureState(ItemState(ctx.guiWidth(), ctx.guiHeight(), scissor, bounds, batchList.toList()))
            batchList.clear()
        }
    }
}