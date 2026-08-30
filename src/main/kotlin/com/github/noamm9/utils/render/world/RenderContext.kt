package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Camera
import net.minecraft.client.renderer.SubmitNodeCollector

data class RenderContext(val matrixStack: PoseStack, val collector: SubmitNodeCollector, val camera: Camera) {
    constructor(ctx: LevelRenderContext): this(ctx.poseStack(), ctx.submitNodeCollector(), mc.gameRenderer.mainCamera())

    companion object {
        fun fromContext(ctx: LevelRenderContext) = RenderContext(ctx)
    }
}
