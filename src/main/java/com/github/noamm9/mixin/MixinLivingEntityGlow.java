package com.github.noamm9.mixin;

import com.github.noamm9.utils.EntityGlowHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Run before other glow mixins to keep our glow visible
@Mixin(value = LivingEntity.class, priority = 900)
public abstract class MixinLivingEntityGlow {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        var entity = (Entity) (Object) this;

        var glowOverride = EntityGlowHandler.getOverride(entity);
        if (glowOverride != null) cir.setReturnValue(glowOverride);
    }
}
