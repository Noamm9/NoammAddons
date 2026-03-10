package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class MixinLightTexture {
    @ModifyExpressionValue(method = "updateLightTexture(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 2))
    private Object injectFullBright(Object original) {
        if (Camera.INSTANCE.enabled && Camera.getFullBright().getValue()) return 99999.0;
        return original;
    }

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float injectAntiDarkness(LocalPlayer instance, Holder<MobEffect> registryEntry, float v) {
        if (Camera.INSTANCE.enabled && Camera.getDisableDarkness().getValue() && registryEntry == MobEffects.DARKNESS) return 0f;
        return instance.getEffectBlendFactor(registryEntry, v);
    }
}

