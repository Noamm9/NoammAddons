package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.LavaToWater;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidStateModelSet.class)
public abstract class MixinFluidStateModelSet {

    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void onGet(FluidState state, CallbackInfoReturnable<FluidModel> cir) {
        if (!LavaToWater.INSTANCE.enabled) return;
        if (state.getType() != Fluids.LAVA && state.getType() != Fluids.FLOWING_LAVA) return;

        FluidStateModelSet self = (FluidStateModelSet)(Object)this;
        FluidModel waterModel = self.get(Fluids.WATER.defaultFluidState());

        if (LavaToWater.INSTANCE.getColorTint().getValue()) {
            int rgb = LavaToWater.INSTANCE.getTintColor().getValue().getRGB() & 0x00FFFFFF | 0xFF000000;
            FluidModel tintedModel = new FluidModel(
                    waterModel.layer(),
                    waterModel.stillMaterial(),
                    waterModel.flowingMaterial(),
                    waterModel.overlayMaterial(),
                    BlockTintSources.constant(rgb, rgb)
            );
            cir.setReturnValue(tintedModel);
        } else {
            cir.setReturnValue(waterModel);
        }
    }
}