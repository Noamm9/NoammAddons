package noammaddons.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import noammaddons.features.impl.dungeons.FullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BlockButton.class)
public class MixinBlockButton extends Block {
    public MixinBlockButton(Material materialIn) {
        super(materialIn);
    }

    @Inject(method = "updateBlockBounds", at = @At("HEAD"), cancellable = true)
    private void onUpdateBlockBounds(IBlockState state, CallbackInfo ci) {
        if (!FullBlock.button.getValue()) return;
        ci.cancel();
        EnumFacing enumfacing = state.getValue(BlockButton.FACING);
        boolean flag = state.getValue(BlockButton.POWERED);
        float f2 = (flag ? 1 : 2) / 16.0f;

        switch (enumfacing) {
            case EAST:
                setBlockBounds(0.0f, 0.0f, 0.0f, f2, 1.0f, 1.0f);
                break;

            case WEST:
                setBlockBounds(1.0f - f2, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
                break;

            case SOUTH:
                setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, f2);
                break;

            case NORTH:
                setBlockBounds(0.0f, 0.0f, 1.0f - f2, 1.0f, 1.0f, 1.0f);
                break;

            case UP:
                setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.0f + f2, 1.0f);
                break;

            case DOWN:
                setBlockBounds(0.0f, 1.0f - f2, 0.0f, 1.0f, 1.0f, 1.0f);
                break;
        }
    }
}