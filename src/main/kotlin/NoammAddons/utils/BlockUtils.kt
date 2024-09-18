package NoammAddons.utils


import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.world.World


object BlockUtils {
    val blacklist = listOf(
        Blocks.acacia_door,
        Blocks.anvil,
        Blocks.beacon,
        Blocks.bed,
        Blocks.birch_door,
        Blocks.brewing_stand,
        Blocks.brown_mushroom,
        Blocks.chest,
        Blocks.command_block,
        Blocks.crafting_table,
        Blocks.dark_oak_door,
        Blocks.daylight_detector,
        Blocks.daylight_detector_inverted,
        Blocks.dispenser,
        Blocks.dropper,
        Blocks.enchanting_table,
        Blocks.ender_chest,
        Blocks.furnace,
//        Blocks.hopper,
        Blocks.jungle_door,
        Blocks.redstone_block,
        Blocks.lever,
        Blocks.noteblock,
        Blocks.oak_door,
        Blocks.powered_comparator,
        Blocks.powered_repeater,
        Blocks.red_mushroom,
        Blocks.skull,
        Blocks.standing_sign,
        Blocks.stone_button,
        Blocks.trapdoor,
        Blocks.trapped_chest,
        Blocks.unpowered_comparator,
        Blocks.unpowered_repeater,
        Blocks.wall_sign,
        Blocks.wooden_button
    )

    fun Block.getName(): String = this.registryName.toString()

    fun World.getBlockAt(pos: BlockPos): Block? = this.getBlockState(pos)?.block

    fun Block.getBlockId(): Int = Block.getIdFromBlock(this)

    val Block.blockBounds: AxisAlignedBB
        get() = AxisAlignedBB(blockBoundsMinX, blockBoundsMinY, blockBoundsMinZ, blockBoundsMaxX, blockBoundsMaxY, blockBoundsMaxZ)

    fun toAir(blockPos: BlockPos?) {
        if (blockPos == null) return
        val block = mc.theWorld.getBlockAt(blockPos)
        if (blacklist.contains(block)) return
        mc.theWorld.setBlockToAir(blockPos)
    }

    fun ghostBlock(blockPos: BlockPos, blockState: IBlockState) {
        mc.theWorld.setBlockState(blockPos, blockState)
    }
}
