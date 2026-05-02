package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.LavaToWater;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public abstract class MixinItemBlockRenderTypes {

    @Inject(
        method = "getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void hookFluidRenderLayer(FluidState state, CallbackInfoReturnable<ChunkSectionLayer> cir) {
        if (LavaToWater.INSTANCE.enabled
                && (state.getType() == Fluids.LAVA || state.getType() == Fluids.FLOWING_LAVA)) {
            cir.setReturnValue(ItemBlockRenderTypes.getRenderLayer(Fluids.WATER.defaultFluidState()));
        }
    }
}
