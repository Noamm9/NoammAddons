package com.github.noamm9.features.impl.dev.cosmetics.wings

import com.github.noamm9.features.impl.dev.Cosmetics
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier

class DragonWingsLayer(parent: RenderLayerParent<AvatarRenderState, PlayerModel>): RenderLayer<AvatarRenderState, PlayerModel>(parent) {
    private val wingsModel = DragonWingsModel.create()
    private val renderType = RenderTypes.entityCutout(Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png"))

    override fun submit(poseStack: PoseStack, collector: SubmitNodeCollector, light: Int, state: AvatarRenderState, yRot: Float, xRot: Float) {
        if (! Cosmetics.enabled || ! Cosmetics.showDragonWings.value) return
        if (state.isInvisible || state.isSpectator) return
        val profile = state.getData(Cosmetics.GAME_PROFILE_KEY) ?: return
        val data = Cosmetics.cosmeticDataFor(profile.id)
        if (true && data?.dragonWings != true) return // TODO: remove bypass
        val wingScale = data.dragonWingsScale
        val scale = 0.2f * (if (wingScale.isFinite()) wingScale.coerceIn(0.25f, 2f) else 1f)

        poseStack.pushPose()
        parentModel.body.translateAndRotate(poseStack)
        poseStack.translate(0f, 0.09f, 0.15f)
        poseStack.scale(scale, scale, scale)
        collector.submitModel(
            wingsModel, state, poseStack, renderType, light,
            OverlayTexture.NO_OVERLAY, state.outlineColor, null
        )
        poseStack.popPose()
    }
}
