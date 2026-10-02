package com.github.noamm9.mixin;

//#if CHEAT

import com.github.noamm9.features.impl.dungeon.IHateDoors;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSectionRegion.class)
public abstract class MixinRenderSectionRegion {
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState replaceDoorRenderState(BlockState original, BlockPos pos) {
        return IHateDoors.getRenderState(pos.getX(), pos.getY(), pos.getZ(), original);
    }
}

//#endif
