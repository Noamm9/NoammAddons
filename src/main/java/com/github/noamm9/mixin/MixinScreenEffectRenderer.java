package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class MixinScreenEffectRenderer {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFire(CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getHideFireOverlay().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void onRenderWater(CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getHideWaterOverlay().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void onRenderBlock(CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getHideBlockOverlay().getValue()) ci.cancel();
    }
}