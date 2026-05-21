package com.github.noamm9.utils.render

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.BlitRenderState
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import java.util.*

@Suppress("CAST_NEVER_SUCCEEDS")
class ItemRenderer(vertexConsumers: MultiBufferSource.BufferSource): PictureInPictureRenderer<ItemRenderer.ItemState>(vertexConsumers) {
    override fun textureIsReadyToBlit(itemState: ItemState) = lastItemState != null && lastItemState == itemState
    override fun getTextureLabel() = NoammAddons.MOD_ID + "_" + this.javaClass.simpleName
    override fun getTranslateY(height: Int, windowScaleFactor: Int) = height / 2f
    override fun getRenderStateClass() = ItemState::class.java

    private var textureView: GpuTextureView? = null
    private var lastItemState: ItemState? = null

    override fun renderToTexture(itemState: ItemState, poseStack: PoseStack) {
        poseStack.scale(1f, - 1f, - 1f)

        if (itemState.state.itemStackRenderState().usesBlockLight()) mc.gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_3D)
        else mc.gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_FLAT)

        val dispatcher = mc.gameRenderer.featureRenderDispatcher
        itemState.state.itemStackRenderState().submit(poseStack, dispatcher.submitNodeStorage, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
        dispatcher.renderAllFeatures()

        lastItemState = itemState
        textureView = RenderSystem.outputColorTextureOverride
    }

    override fun blitTexture(element: ItemState, state: GuiRenderState) {
        state.submitBlitToCurrentLayer(BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(textureView !!, RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)),
            element.pose(), element.x0(), element.y0(), element.x0() + 16, element.y0() + 16,
            0.0f, 1.0f, 1.0f, 0.0f, - 1, element.scissorArea(), null
        ))
    }

    data class ItemState(val state: GuiItemRenderState): PictureInPictureRenderState {
        override fun x0() = state.x()
        override fun y0() = state.y()
        override fun x1() = state.x() + scale().toInt()
        override fun y1() = state.y() + scale().toInt()
        override fun scale() = maxOf(state.pose().m00(), state.pose().m11()) * 16f
        override fun scissorArea() = state.scissorArea()
        override fun bounds() = state.bounds()
        override fun pose() = state.pose()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ItemState) return false
            if (other.state.itemStackRenderState().modelIdentity != state.itemStackRenderState().modelIdentity) return false
            if (other.state.pose().m00() != state.pose().m00()) return false
            if (other.state.pose().m11() != state.pose().m11()) return false
            return true
        }

        override fun hashCode() = Objects.hash(state.itemStackRenderState().modelIdentity, state.pose().m00(), state.pose().m11())
    }

    companion object {
        fun GuiGraphics.drawItemStack(item: ItemStack, x: Int, y: Int) {
            val tracking = TrackingItemStackRenderState()
            mc.itemModelResolver.updateForTopItem(tracking, item, ItemDisplayContext.GUI, mc.level, mc.player, 0)
            val itemState = ItemState(GuiItemRenderState(item.item.name.string, Matrix3x2f(pose()), tracking, x, y, scissorStack.peek()))
            guiRenderState.submitPicturesInPictureState(itemState)
        }
    }
}