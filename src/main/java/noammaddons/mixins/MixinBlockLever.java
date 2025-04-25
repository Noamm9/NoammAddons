package noammaddons.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import noammaddons.features.impl.dungeons.FullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


// Taken from Odin mod
// Under BSD 3-Clause License
@Mixin(BlockLever.class)
public class MixinBlockLever extends Block {
    public MixinBlockLever(Material materialIn) {
        super(materialIn);
    }

    @Inject(method = "setBlockBoundsBasedOnState", at = @At("HEAD"), cancellable = true)
    private void onSetBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos, CallbackInfo ci) {
        if (!FullBlock.lever.getValue()) return;
        this.setBlockBounds(0, 0, 0, 1, 1, 1);
        ci.cancel();
    }
}