package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Tweaks;
import com.github.noamm9.utils.location.LocationUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.noamm9.NoammAddons.mc;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(method = "applyAfterUseComponentSideEffects", at = @At("HEAD"), cancellable = true)
    private void onApplyCooldown(LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (Tweaks.INSTANCE.enabled && user == mc.player && LocationUtils.inSkyblock) {
            if (stack.is(Items.ENDER_PEARL)) {
                cir.setReturnValue(stack);
            }
        }
    }

    @WrapOperation(
            method = "applyAfterUseComponentSideEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/UseCooldown;apply(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V")
    )
    private void onApplyCooldown(UseCooldown instance, ItemStack stack, LivingEntity user, Operation<Void> original) {
        if (Tweaks.INSTANCE.enabled && Tweaks.getHideItemCooldowns().getValue() && user == mc.player && LocationUtils.inSkyblock && !stack.is(Items.ENDER_PEARL)) {
            return;
        }

        original.call(instance, stack, user);
    }
}
