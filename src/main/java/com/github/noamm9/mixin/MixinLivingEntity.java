package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.visual.Animations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    public MixinLivingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void overrideSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!Animations.INSTANCE.enabled) return;
        if (!Animations.INSTANCE.getIgnoreHaste().getValue()) return;
        if (!this.is(NoammAddons.mc.player)) return;
        if (NoammAddons.mc.player.getMainHandItem().isEmpty()) return;

        cir.setReturnValue(Animations.INSTANCE.getSwingSpeed().getValue());
    }
}
