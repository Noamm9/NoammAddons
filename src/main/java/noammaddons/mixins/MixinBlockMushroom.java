package noammaddons.mixins;

import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockMushroom;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import noammaddons.features.impl.misc.FullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(BlockMushroom.class)
public class MixinBlockMushroom extends BlockBush {

    @Unique
    private void noammAddons$setFullBlock() {
        setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
        if (FullBlock.mushroom.getValue()) noammAddons$setFullBlock();
        return super.getSelectedBoundingBox(worldIn, pos);
    }

    @Override
    public MovingObjectPosition collisionRayTrace(World worldIn, BlockPos pos, Vec3 start, Vec3 end) {
        if (FullBlock.mushroom.getValue()) noammAddons$setFullBlock();
        return super.collisionRayTrace(worldIn, pos, start, end);
    }
}