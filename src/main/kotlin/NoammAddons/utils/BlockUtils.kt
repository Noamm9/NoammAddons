package NoammAddons.utils

import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraft.world.World

object BlockUtils {

    fun Block.getName(): String = this.registryName.toString()

    fun World.getBlockAt(pos: BlockPos): Block = this.getBlockState(pos).block

    fun Block.getBlockId(): Int = Block.getIdFromBlock(this)
}