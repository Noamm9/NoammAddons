package noammaddons.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import noammaddons.features.impl.misc.FullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.block.BlockSkull.FACING;
import static noammaddons.utils.DungeonUtils.isSecret;


@Mixin(BlockSkull.class)
public class MixinBlockSkull extends Block {
    public MixinBlockSkull(Material materialIn) {
        super(materialIn);
    }

    @Inject(method = "setBlockBoundsBasedOnState", at = @At("HEAD"), cancellable = true)
    private void onSetBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos, CallbackInfo ci) {
        if (!FullBlock.skull.getValue()) return;
        if (!isSecret(pos)) return;

        setBlockBounds(0, 0, 0, 1, 1, 1);
        ci.cancel();
    }

    @Inject(method = "getCollisionBoundingBox", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state, CallbackInfoReturnable<AxisAlignedBB> cir) {
        if (!FullBlock.skull.getValue()) return;

        switch (worldIn.getBlockState(pos).getValue(FACING)) {
            case NORTH: {
                setBlockBounds(0.25f, 0.25f, 0.5f, 0.75f, 0.75f, 1.0f);
                break;
            }
            case SOUTH: {
                setBlockBounds(0.25f, 0.25f, 0.0f, 0.75f, 0.75f, 0.5f);
                break;
            }
            case WEST: {
                setBlockBounds(0.5f, 0.25f, 0.25f, 1.0f, 0.75f, 0.75f);
                break;
            }
            case EAST: {
                setBlockBounds(0.0f, 0.25f, 0.25f, 0.5f, 0.75f, 0.75f);
            }
            default: {
                setBlockBounds(0.25f, 0.0f, 0.25f, 0.75f, 0.5f, 0.75f);
                break;
            }
        }

        AxisAlignedBB collisionBoundingBox = super.getCollisionBoundingBox(worldIn, pos, state);
        setBlockBounds(0, 0, 0, 1, 1, 1);
        cir.setReturnValue(collisionBoundingBox);
        cir.cancel();

    }
}