package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void hideDinnerboneNametag(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (NameTagTweaks.shouldHideDinnerbone(entity)) cir.setReturnValue(false);
    }
}
