package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.LavaToWater;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidStateModelSet.class)
public abstract class MixinFluidStateModelSet {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void onGet(FluidState state, CallbackInfoReturnable<FluidModel> cir) {
        LavaToWater.modelHook((FluidStateModelSet) (Object) this, state, cir);
    }
}