package noammaddons.utils


import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noammaddons.noammaddons.Companion.mc


object BlockUtils {
    val blackList = listOf(
        crafting_table, anvil, ender_chest, chest, lever, acacia_door, beacon,
        bed, birch_door, brewing_stand, brown_mushroom, command_block, dark_oak_door,
        daylight_detector, daylight_detector_inverted, dispenser, dropper, enchanting_table,
        furnace, jungle_door, redstone_block, noteblock, oak_door, powered_comparator,
        powered_repeater, red_mushroom, skull, standing_sign, trapdoor, unpowered_comparator,
        unpowered_repeater, wall_sign, trapped_chest, stone_button, wooden_button
    )

    fun getStateAt(pos: BlockPos) = mc.theWorld?.getBlockState(pos) ?: air.defaultState
    fun getBlockAt(pos: BlockPos) = getStateAt(pos).block
    fun getBlockAt(vec3: Vec3) = getBlockAt(vec3.toPos())
    fun getBlockAt(x: Number, y: Number, z: Number) = getBlockAt(BlockPos(x.toDouble(), y.toDouble(), z.toDouble()))

    fun Block.getBlockId(): Int = Block.getIdFromBlock(this)

    fun toAir(blockPos: BlockPos?) {
        if (blockPos == null) return
        val block = getBlockAt(blockPos)
        if (blackList.contains(block)) return
        mc.theWorld?.setBlockToAir(blockPos)
    }

    fun ghostBlock(blockPos: BlockPos, blockState: IBlockState) {
        mc.theWorld?.setBlockState(blockPos, blockState)
    }

    fun BlockPos.toVec() = Vec3(this)
    fun Vec3.toPos() = BlockPos(this)

    fun IBlockState.getMetadata(): Int = block.getMetaFromState(this)

    fun IBlockState.getBlockId(): Int = block.getBlockId()
}
