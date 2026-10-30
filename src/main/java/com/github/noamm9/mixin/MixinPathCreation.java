package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Tweaks;
import com.llamalad7.mixinextras.sugar.Local;
//~ if <26.3 'core.component.BlockTransformer' -> 'world.item.ShovelItem'
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//~ if <26.3 'BlockTransformer.class' -> 'ShovelItem.class'
@Mixin(BlockTransformer.class)
public class MixinPathCreation {
    //? if >=26.3 {
    @Inject(method = "transformBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/context/UseOnContext;getPlayer()Lnet/minecraft/world/entity/player/Player;"), cancellable = true)
    //? } else {
    /*@Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), cancellable = true)
    *///? }
    //~ if <26.3 'cir, @Local(ordinal = 0) BlockState newState' -> 'cir'
    private void preventAotvPaths(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir, @Local(ordinal = 0) BlockState newState) {
        //? if >=26.3 {
        if (! newState.is(Blocks.DIRT_PATH)) return;
        //? }
        if (context.getLevel().isClientSide() && Tweaks.shouldPreventAotvPaths(context.getItemInHand())) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
