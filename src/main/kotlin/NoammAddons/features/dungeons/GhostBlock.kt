package NoammAddons.features.dungeons

import net.minecraft.init.Blocks
import net.minecraft.item.ItemPickaxe
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.NoammAddons.Companion.keybinds
import NoammAddons.events.ClickEvent
import NoammAddons.utils.LocationUtils.inSkyblock


object GhostBlock {
    private val blacklist = listOf(
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
        Blocks.hopper,
        Blocks.jungle_door,
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

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !inSkyblock || !keybinds[0].isKeyDown) return
        toAir(mc.objectMouseOver.blockPos)
    }

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (!config.stonkGhostBlock || !inSkyblock || mc.objectMouseOver == null) return
        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (mc.thePlayer.heldItem?.item is ItemPickaxe) {
                event.isCanceled = toAir(mc.objectMouseOver.blockPos)
            }
        }
    }

    private fun toAir(blockPos: BlockPos?): Boolean {
        if (blockPos != null) {
            val block = mc.theWorld.getBlockState(mc.objectMouseOver.blockPos).block
            if (!blacklist.contains(block)) {
                mc.theWorld.setBlockToAir(mc.objectMouseOver.blockPos)
                return true
            }
        }
        return false
    }
}
