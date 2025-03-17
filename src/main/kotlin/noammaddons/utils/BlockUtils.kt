package noammaddons.utils


import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noammaddons.noammaddons.Companion.mc


object BlockUtils {
    val blackList = listOf(
        Blocks.crafting_table,
        Blocks.anvil,
        Blocks.ender_chest,
        Blocks.chest,
        Blocks.lever,
        Blocks.acacia_door,
        Blocks.beacon,
        Blocks.bed,
        Blocks.birch_door,
        Blocks.brewing_stand,
        Blocks.brown_mushroom,
        Blocks.command_block,
        Blocks.dark_oak_door,
        Blocks.daylight_detector,
        Blocks.daylight_detector_inverted,
        Blocks.dispenser,
        Blocks.dropper,
        Blocks.enchanting_table,
        Blocks.furnace,
        Blocks.jungle_door,
        Blocks.redstone_block,
        Blocks.noteblock,
        Blocks.oak_door,
        Blocks.powered_comparator,
        Blocks.powered_repeater,
        Blocks.red_mushroom,
        Blocks.skull,
        Blocks.standing_sign,
        Blocks.trapdoor,
        Blocks.unpowered_comparator,
        Blocks.unpowered_repeater,
        Blocks.wall_sign,
        Blocks.trapped_chest,
        Blocks.stone_button,
        Blocks.wooden_button
    )

    fun getStateAt(pos: BlockPos) = mc.theWorld?.getBlockState(pos) ?: air.defaultState
    fun getStateAt(x: Number, y: Number, z: Number) = getStateAt(BlockPos(x.toDouble(), y.toDouble(), z.toDouble()))
    fun getBlockAt(pos: BlockPos) = getStateAt(pos).block
    fun getBlockAt(vec3: Vec3) = getBlockAt(vec3.toPos())
    fun getBlockAt(x: Number, y: Number, z: Number) = getBlockAt(BlockPos(x.toDouble(), y.toDouble(), z.toDouble()))

    fun Block.getBlockId(): Int = Block.getIdFromBlock(this)

    fun toAir(blockPos: BlockPos?) {
        if (blockPos == null) return
        val block = getBlockAt(blockPos)
        if (blackList.contains(block)) return
        mc.theWorld.setBlockToAir(blockPos)
    }

    fun ghostBlock(blockPos: BlockPos, blockState: IBlockState) {
        mc.theWorld.setBlockState(blockPos, blockState)
    }

    fun BlockPos.toVec() = Vec3(this)
    fun Vec3.toPos() = BlockPos(this)

    fun IBlockState.getMetadata(): Int = block.getMetaFromState(this)

    fun IBlockState.getBlockId(): Int = block.getBlockId()
}
