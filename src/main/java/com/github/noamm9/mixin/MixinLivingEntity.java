package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.visual.Animations;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    public MixinLivingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> holder);

    @Shadow
    public abstract @Nullable MobEffectInstance getEffect(Holder<MobEffect> holder);

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void adjustSwingLength(CallbackInfoReturnable<Integer> cir) {
        if (!Animations.INSTANCE.enabled) return;
        if (!this.is(NoammAddons.mc.player)) return;
        if (NoammAddons.mc.player.getMainHandItem() == ItemStack.EMPTY) return;

        int length;

        if (Animations.INSTANCE.getIgnoreHaste().getValue()) length = 6;
        else if (this.hasEffect(MobEffects.HASTE)) length = 6 - (1 + this.getEffect(MobEffects.HASTE).getAmplifier());
        else if (this.hasEffect(MobEffects.MINING_FATIGUE))
            length = 6 + (1 + this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2;
        else length = 6;

        double speedMod = Animations.INSTANCE.getSwingSpeed().getValue().doubleValue();
        int finalLength = (int) (length * Math.exp(-(speedMod)));

        cir.setReturnValue(Math.max(finalLength, 1));
    }
}
