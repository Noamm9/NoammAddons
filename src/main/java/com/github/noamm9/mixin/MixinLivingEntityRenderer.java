package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {
    @ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDiscrete()Z"))
    private boolean forceSneakingNametag(boolean original, LivingEntity entity) {
        return NameTagTweaks.isForceNametagActive() && entity instanceof Player ? false : original;
    }

    @ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean forceInvisibleNametag(boolean original, LivingEntity entity) {
        return NameTagTweaks.isForceNametagActive() && entity instanceof Player && entity.getUUID().version() == 4 ? false : original;
    }
}
