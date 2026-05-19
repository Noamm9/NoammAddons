package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NoItemPlace;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {
    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void placeBlockHook(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (NoItemPlace.placeHook(context)) cir.setReturnValue(true);
    }

    @Inject(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"), cancellable = true)
    private void useHook(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (NoItemPlace.placeHook(context)) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}