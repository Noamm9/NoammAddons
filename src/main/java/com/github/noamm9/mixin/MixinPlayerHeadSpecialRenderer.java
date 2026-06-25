package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.PackDisabler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerHeadSpecialRenderer.class)
public class MixinPlayerHeadSpecialRenderer {
    @WrapOperation(method = "extractArgument*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object replaceSkyblockHeadProfile(ItemStack instance, DataComponentType dataComponentType, Operation<ResolvableProfile> original) {
        return PackDisabler.skullProfileHook(instance, dataComponentType, original);
    }
}