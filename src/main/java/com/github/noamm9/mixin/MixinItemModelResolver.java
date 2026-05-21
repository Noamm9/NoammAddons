package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.RevertAxes;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public class MixinItemModelResolver {
    @ModifyVariable(method = "updateForLiving", at = @At("HEAD"), argsOnly = true)
    private ItemStack revertAxe(ItemStack original) {
        return RevertAxes.shouldReplace(original);
    }
}