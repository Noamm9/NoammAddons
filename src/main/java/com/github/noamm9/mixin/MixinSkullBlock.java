package com.github.noamm9.mixin;

//#if CHEAT

import com.github.noamm9.features.impl.dungeon.SecretHitboxes;
import com.github.noamm9.utils.dungeons.DungeonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlock.class)
abstract class MixinSkullBlock extends AbstractSkullBlock {
    @Shadow @Final private static VoxelShape SHAPE_PIGLIN;

    @Shadow @Final private static VoxelShape SHAPE;

    public MixinSkullBlock(SkullBlock.Type type, Properties properties) {
        super(type, properties);
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void modifyShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (SecretHitboxes.INSTANCE.enabled && SecretHitboxes.getSkull().getValue() && DungeonUtils.isSecret(pos)) {
            cir.setReturnValue(Shapes.block());
        }
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getType() == SkullBlock.Types.PIGLIN ? SHAPE_PIGLIN : SHAPE;
    }
}
//#endif
