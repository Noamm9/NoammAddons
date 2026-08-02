package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.OldMasterStar;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @WrapOperation(
        method = "getCustomName",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;", ordinal = 0)
    )
    private Object oldMasterStarHook(ItemStack instance, DataComponentType<?> key, Operation<Object> original) {
        Object orig = original.call(instance, key);
        if (key != DataComponents.CUSTOM_NAME || !(orig instanceof Component)) return orig;

        return OldMasterStar.transformName((Component) orig);
    }
}
