package com.github.noamm9.mixin;

//#if CHEAT

import com.github.noamm9.features.impl.dungeon.SecretHitboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ButtonBlock.class)
public class MixinButtonBlock {
    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void modifyShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (SecretHitboxes.INSTANCE.enabled && SecretHitboxes.getButton().getValue()) {
            cir.setReturnValue(SecretHitboxes.getButtonShape(state));
        }
    }
}
//#endif