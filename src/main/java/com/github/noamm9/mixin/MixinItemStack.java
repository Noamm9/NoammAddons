package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Tweaks;
import com.github.noamm9.utils.location.LocationUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.noamm9.NoammAddons.mc;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(method = "applyAfterUseComponentSideEffects", at = @At("HEAD"), cancellable = true)
    private void onApplyCooldown(LivingEntity user, ItemStack stackBeforeUsing, CallbackInfoReturnable<ItemStack> cir) {
        if (!Tweaks.INSTANCE.enabled || !LocationUtils.inSkyblock || user != mc.player) return;
        if (stackBeforeUsing.is(Items.ENDER_PEARL)) {
            cir.setReturnValue(stackBeforeUsing);
        }
    }
}