package com.github.noamm9.features.impl.dev.cosmetics.halo

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.dev.Cosmetics
import com.github.noamm9.features.impl.dev.Cosmetics.GAME_PROFILE_KEY
import com.github.noamm9.features.impl.dev.Cosmetics.SNEAKING_KEY
import com.github.noamm9.features.impl.dev.Cosmetics.showHalo
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import java.awt.Color
import kotlin.math.sin

class HaloLayer(parent: RenderLayerParent<AvatarRenderState, PlayerModel>): RenderLayer<AvatarRenderState, PlayerModel>(parent) {
    private val haloModel = HaloModel.create()
    private val whiteTexture = Identifier.fromNamespaceAndPath(NoammAddons.MOD_ID, "halo").also {
        val texture = DynamicTexture("noamm_halo", 1, 1, false)
        texture.pixels.setPixel(0, 0, - 1)
        texture.upload()
        mc.textureManager.register(it, texture)
    }

    override fun submit(poseStack: PoseStack, collector: SubmitNodeCollector, light: Int, state: AvatarRenderState, yRot: Float, xRot: Float) {
        if (! showHalo.value) return
        if (state.isInvisibleToPlayer) return
        val profile = state.getData(GAME_PROFILE_KEY) ?: return
        val data = Cosmetics.cosmeticDataFor(profile.id) ?: return
        if (! data.hasHalo) return

        val bobOffset = (sin(System.currentTimeMillis() % 2000L / 2000f * (Math.PI.toFloat() * 2f)) + 1f) / 2f * 0.08f
        val offset = if (state.getData(SNEAKING_KEY) == true) 0.5f else 0.7f

        poseStack.pushPose()
        poseStack.translate(0f, - offset, 0f)
        poseStack.translate(0f, - (bobOffset * 2f), 0f)
        collector.submitModel(
            haloModel, state, poseStack,
            RenderTypes.entitySolid(whiteTexture),
            LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, data.halo ?: Color.YELLOW.rgb,
            null, 0, null
        )
        poseStack.popPose()
    }
}
