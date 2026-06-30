package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.LavaToWater;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LavaFogEnvironment.class)
public abstract class MixinLavaFogEnvironment {
    @Unique
    private static final WaterFogEnvironment WATER_FOG = new WaterFogEnvironment();

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void hookSetupFog(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!LavaToWater.INSTANCE.enabled) return;
        WATER_FOG.setupFog(fogData, camera, clientLevel, f, deltaTracker);
        ci.cancel();
    }

    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void hookGetBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        if (!LavaToWater.INSTANCE.enabled) return;
        cir.setReturnValue(WATER_FOG.getBaseColor(level, camera, renderDistance, partialTicks));
    }
}