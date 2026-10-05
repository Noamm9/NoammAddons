package com.github.noamm9.mixin;

//#if CHEAT

import com.github.noamm9.features.impl.dungeon.IHateDoors;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public abstract class MixinSodiumLevelSlice {
    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"))
    private BlockState replaceDoorRenderState(BlockState original, int x, int y, int z) {
        return IHateDoors.getRenderState(x, y, z, original);
    }
}
//#endif