package com.github.noamm9.mixin;

//#if CHEAT

import com.github.noamm9.features.impl.misc.Camera;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MixinMobEffectFogEnvironment {
    @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
    public void hookGetStatusEffect(CallbackInfoReturnable<Boolean> cir) {
        if (Camera.INSTANCE.enabled && Camera.getDisableBlindness().getValue()) {
            cir.setReturnValue(false);
        }
    }
}
//#endif