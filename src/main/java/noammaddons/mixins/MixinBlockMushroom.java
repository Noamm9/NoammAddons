package noammaddons.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockMushroom;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import noammaddons.utils.BlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static noammaddons.noammaddons.config;


// Taken from Odin mod
// Under BSD 3-Clause License
@Mixin(BlockMushroom.class)
public class MixinBlockMushroom extends BlockBush {

    @Unique
    private void noammAddons$setFullBlock(Block block) {
        block.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        if (config.getFullblockMushroom()) noammAddons$setFullBlock(BlockUtils.INSTANCE.getBlockAt(pos));
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        if (config.getFullblockMushroom()) noammAddons$setFullBlock(BlockUtils.INSTANCE.getBlockAt(pos));
        return super.collisionRayTrace(worldIn, pos, start, end);
    }
}