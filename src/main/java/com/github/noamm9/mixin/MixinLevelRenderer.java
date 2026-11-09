package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.misc.BlockOverlay;
import com.github.noamm9.utils.render.world.RenderBatcher;
import com.github.noamm9.utils.render.world.RenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if <26.3 {
/*import com.mojang.blaze3d.pipeline.RenderTarget;
*///?} else {
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//?}

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Shadow @Final private GameRenderer gameRenderer;

    //? if <26.3 {
    /*@Shadow @Final private RenderTarget entityOutlineTarget;

    @Inject(
        method = "/lambda\\$addMainPass\\$\\d+/",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
            shift = At.Shift.AFTER
        )
    )
    private void copyWorldDepthToOutline(CallbackInfo ci) {
        if (NoammAddons.isCheat) return;
        entityOutlineTarget.copyDepthFrom(gameRenderer.mainRenderTarget());
    }
    *///?} else {

    // 26.3 skips the always on top phase unless vanilla has its own gizmos queued that frame
    @Inject(method = "frameHasAlwaysOnTopGizmos", at = @At("HEAD"), cancellable = true)
    private void forceAlwaysOnTopPass(CallbackInfoReturnable<Boolean> cir) {
        if (RenderBatcher.hasAlwaysOnTop) cir.setReturnValue(true);
    }

    @ModifyArg(
        method = "executeOutline",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"
        ),
        index = 3
    )
    private GpuTextureView bindWorldDepthToOutline(GpuTextureView depth) {
        if (NoammAddons.isCheat) return depth;
        return gameRenderer.mainRenderTarget().getDepthTextureView();
    }
    //?}

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onSubmitBlockOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (! BlockOverlay.INSTANCE.enabled) return;
        var state = levelRenderState.blockOutlineRenderState;
        if (state == null) return;

        var ctx = new RenderContext(poseStack, levelRenderState.cameraRenderState, submitNodeCollector);

        BlockOverlay.renderOverlay(ctx, state);
        ci.cancel();
    }
}