package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlindnessFogEnvironment.class)
public abstract class MixinBlindnessFogEnvironment {
    @Inject(method = "getMobEffect()Lnet/minecraft/core/Holder;", at = @At("HEAD"), cancellable = true)
    public void hookGetStatusEffect(CallbackInfoReturnable<Holder<MobEffect>> cir) {
        if (Camera.INSTANCE.enabled && Camera.getDisableBlindness().getValue()) {
            cir.setReturnValue(null);
        }
    }
}