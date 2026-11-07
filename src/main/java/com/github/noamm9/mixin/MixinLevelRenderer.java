package com.github.noamm9.mixin;


import com.github.noamm9.features.impl.misc.BlockOverlay;
import com.github.noamm9.utils.render.world.RenderBatcher;
import com.github.noamm9.utils.render.world.RenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    //? if >=26.3 {
    // 26.3 skips the always on top phase unless vanilla has its own gizmos queued that frame
    @Inject(method = "frameHasAlwaysOnTopGizmos", at = @At("HEAD"), cancellable = true)
    private void forceAlwaysOnTopPass(CallbackInfoReturnable<Boolean> cir) {
        if (RenderBatcher.hasAlwaysOnTop) cir.setReturnValue(true);
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