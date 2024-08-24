package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.BlockUtils.getBlockAt
import NoammAddons.utils.BlockUtils.getName
import NoammAddons.utils.LocationUtils.F7Phase
import NoammAddons.utils.LocationUtils.dungeonFloor
import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object IHATEDIORITE {

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (dungeonFloor == 7 && F7Phase == 2 && config.IHATEDIORITE) {
            for (i in 0..37) {
                GreenArray.forEach { block ->
                    if (mc.theWorld.getBlockAt(block.add(0,i,0)).getName() == "minecraft:stone") {
                        mc.theWorld.setBlockState(block.add(0,i,0), WhileGlass)
                    }
                }

                YellowArray.forEach { block ->
                    if (mc.theWorld.getBlockAt(block.add(0,i,0)).getName() == "minecraft:stone") {
                        mc.theWorld.setBlockState(block.add(0,i,0), WhileGlass)
                    }
                }

                BlueArray.forEach { block ->
                    if (mc.theWorld.getBlockAt(block.add(0,i,0)).getName() == "minecraft:stone") {
                        mc.theWorld.setBlockState(block.add(0,i,0), WhileGlass)
                    }
                }
            }
        }
    }


    /*@SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (mc.objectMouseOver == null) return
        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            ModMessage(mc.objectMouseOver.blockPos.toString())
        }
    }*/


    private val WhileGlass = Block.getBlockFromName("stained_glass").blockState.baseState

    private val GreenArray = arrayOf(
        BlockPos(45, 169, 44),
        BlockPos(46, 169, 44),
        BlockPos(47, 169, 44),
        BlockPos(44, 169, 43),
        BlockPos(45, 169, 43),
        BlockPos(46, 169, 43),
        BlockPos(47, 169, 43),
        BlockPos(48, 169, 43),
        BlockPos(43, 169, 42),
        BlockPos(44, 169, 42),
        BlockPos(45, 169, 42),
        BlockPos(46, 169, 42),
        BlockPos(47, 169, 42),
        BlockPos(48, 169, 42),
        BlockPos(49, 169, 42),
        BlockPos(43, 169, 41),
        BlockPos(44, 169, 41),
        BlockPos(45, 169, 41),
        BlockPos(46, 169, 41),
        BlockPos(47, 169, 41),
        BlockPos(48, 169, 41),
        BlockPos(49, 169, 41),
        BlockPos(43, 169, 40),
        BlockPos(44, 169, 40),
        BlockPos(45, 169, 40),
        BlockPos(46, 169, 40),
        BlockPos(47, 169, 40),
        BlockPos(48, 169, 40),
        BlockPos(49, 169, 40),
        BlockPos(44, 169, 39),
        BlockPos(45, 169, 39),
        BlockPos(46, 169, 39),
        BlockPos(47, 169, 39),
        BlockPos(48, 169, 39),
        BlockPos(45, 169, 38),
        BlockPos(46, 169, 38),
        BlockPos(47, 169, 38)
    )

    private val YellowArray = arrayOf(
        BlockPos(45, 169, 68),
        BlockPos(46, 169, 68),
        BlockPos(47, 169, 68),
        BlockPos(44, 169, 67),
        BlockPos(45, 169, 67),
        BlockPos(46, 169, 67),
        BlockPos(47, 169, 67),
        BlockPos(48, 169, 67),
        BlockPos(43, 169, 66),
        BlockPos(44, 169, 66),
        BlockPos(45, 169, 66),
        BlockPos(46, 169, 66),
        BlockPos(47, 169, 66),
        BlockPos(48, 169, 66),
        BlockPos(49, 169, 66),
        BlockPos(43, 169, 65),
        BlockPos(44, 169, 65),
        BlockPos(45, 169, 65),
        BlockPos(46, 169, 65),
        BlockPos(47, 169, 65),
        BlockPos(48, 169, 65),
        BlockPos(49, 169, 65),
        BlockPos(43, 169, 64),
        BlockPos(44, 169, 64),
        BlockPos(45, 169, 64),
        BlockPos(46, 169, 64),
        BlockPos(47, 169, 64),
        BlockPos(48, 169, 64),
        BlockPos(49, 169, 64),
        BlockPos(44, 169, 63),
        BlockPos(45, 169, 63),
        BlockPos(46, 169, 63),
        BlockPos(47, 169, 63),
        BlockPos(48, 169, 63),
        BlockPos(45, 169, 62),
        BlockPos(46, 169, 62),
        BlockPos(47, 169, 62)
    )

    private val BlueArray = arrayOf(
        BlockPos(97, 169, 64),
        BlockPos(97, 169, 65),
        BlockPos(97, 169, 66),
        BlockPos(98, 169, 67),
        BlockPos(98, 169, 66),
        BlockPos(98, 169, 65),
        BlockPos(98, 169, 64),
        BlockPos(98, 169, 63),
        BlockPos(99, 169, 62),
        BlockPos(99, 169, 63),
        BlockPos(99, 169, 64),
        BlockPos(99, 169, 65),
        BlockPos(99, 169, 66),
        BlockPos(99, 169, 67),
        BlockPos(99, 169, 68),
        BlockPos(100, 169, 68),
        BlockPos(100, 169, 67),
        BlockPos(100, 169, 66),
        BlockPos(100, 169, 65),
        BlockPos(100, 169, 64),
        BlockPos(100, 169, 63),
        BlockPos(100, 169, 62),
        BlockPos(101, 169, 62),
        BlockPos(101, 169, 63),
        BlockPos(101, 169, 64),
        BlockPos(101, 169, 65),
        BlockPos(101, 169, 66),
        BlockPos(101, 169, 67),
        BlockPos(101, 169, 68),
        BlockPos(102, 169, 67),
        BlockPos(102, 169, 66),
        BlockPos(103, 169, 66),
        BlockPos(103, 169, 65),
        BlockPos(102, 169, 65),
        BlockPos(102, 169, 64),
        BlockPos(103, 169, 64),
        BlockPos(102, 169, 63)
    )

}