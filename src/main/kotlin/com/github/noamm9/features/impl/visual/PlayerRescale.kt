package com.github.noamm9.features.impl.visual

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.SliderSetting
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Avatar
import com.mojang.blaze3d.vertex.PoseStack

object PlayerRescale : Feature("Rescales your own player model.") {
    private val scale by SliderSetting("Scale", 1.0f, 0.1f, 1.0f, 0.05f, "x")

    private val IS_LOCAL_PLAYER_KEY = RenderStateDataKey.create<Boolean> { "${NoammAddons.MOD_ID}:is_local_player" }

    @JvmStatic
    fun extractRenderStateHook(avatar: Avatar, state: AvatarRenderState) {
        state.setData(IS_LOCAL_PLAYER_KEY, avatar == NoammAddons.mc.player)
    }

    @JvmStatic
    fun scaleHook(state: AvatarRenderState, poseStack: PoseStack) {
        if (!enabled) return
        if (state.getData(IS_LOCAL_PLAYER_KEY) != true) return

        val size = scale.value
        poseStack.scale(size, size, size)
    }

}