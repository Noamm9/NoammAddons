package com.github.noamm9.utils.render.world

import com.github.noamm9.NoammAddons.mc
import com.mojang.blaze3d.vertex.PoseStack
import gg.essential.universal.UMatrixStack
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.renderer.state.level.CameraRenderState

class RenderContext(val matrixStack: PoseStack, val camera: CameraRenderState) {
    constructor(ctx: LevelRenderContext): this(ctx.poseStack(), mc.gameRenderer.gameRenderState.levelRenderState.cameraRenderState)
    
    fun uMatrixStack() = UMatrixStack(matrixStack.last())
}