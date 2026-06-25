package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.RevertAxes;
import com.github.noamm9.init.NetworkLoop;
import com.github.noamm9.utils.items.ItemUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;

@Mixin(ItemModelResolver.class)
public class MixinItemModelResolver {
    @ModifyVariable(method = "updateForLiving", at = @At("HEAD"), argsOnly = true)
    private ItemStack revertAxe(ItemStack original) {
        return RevertAxes.shouldReplace(original);
    }

    @WrapOperation(method = "appendItemLayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object appendItemLayerHook(ItemStack instance, DataComponentType dataComponentType, Operation<Identifier> original) {
        var currentModel = original.call(instance, dataComponentType);
        if (!currentModel.toString().contains("hypixel_skyblock")) return currentModel;
        var sbid = ItemUtils.INSTANCE.getSkyblockId(instance);
        var oldModel = NetworkLoop.idToLocation.get(sbid);
        return Objects.requireNonNullElse(oldModel, currentModel);
    }
}