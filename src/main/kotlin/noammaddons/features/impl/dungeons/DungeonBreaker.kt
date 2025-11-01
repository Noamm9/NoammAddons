package noammaddons.features.impl.dungeons

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.LocationUtils
import noammaddons.utils.ServerPlayer

object DungeonBreaker: Feature() {
    private val zeropingbreaker by ToggleSetting("Zero Ping")

    private val blacklist = setOf(
        Blocks.barrier, Blocks.bedrock, Blocks.command_block, Blocks.skull, Blocks.tnt, Blocks.chest,
        Blocks.trapped_chest, Blocks.end_portal_frame, Blocks.end_portal, Blocks.sticky_piston,
        Blocks.piston_head, Blocks.piston, Blocks.piston_extension, Blocks.lever, Blocks.stone_button
    )

    @JvmStatic
    fun onHitBlock(pos: BlockPos) {
        if (! enabled) return
        if (! zeropingbreaker) return
        if (! LocationUtils.inDungeon) return
        if (ServerPlayer.player.getHeldItem().skyblockID != "DUNGEONBREAKER") return
        val block = getBlockAt(pos).takeUnless { it in blacklist } ?: return

        mc.theWorld.setBlockToAir(pos)
        mc.theWorld.playSound(
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5,
            block.stepSound.breakSound,
            (block.stepSound.volume + 1.0f) / 2.0f,
            block.stepSound.frequency * 0.8f,
            false
        )
    }
}
