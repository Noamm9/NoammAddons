package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.BlockUtils.getBlockAt
import NoammAddons.utils.BlockUtils.getBlockId
import NoammAddons.utils.ItemUtils.SkyblockID
import NoammAddons.utils.RenderUtils
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object GyroCircle {
    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!config.ShowGyroCircle) return
        if (mc.thePlayer?.heldItem?.SkyblockID != "GYROKINETIC_WAND") return

        val Blockpos = mc.thePlayer.rayTrace(25.0, event.partialTicks)?.blockPos ?: return
        val BlockIdAtPos = mc.theWorld.getBlockAt(Blockpos)?.getBlockId() ?: 0
        val BlockIdAbovePos = mc.theWorld.getBlockAt(Blockpos.add(.0, 1.0, .0))?.getBlockId() ?: 0

        if (BlockIdAtPos != 0 && (BlockIdAbovePos == 0 || BlockIdAbovePos == 171)) {
            RenderUtils.drawBlockBox(blockPos = Blockpos, Color(0, 255, 0, 85), outline = true, fill = true, phase = true)
            RenderUtils.drawCylinder(
                Blockpos.add(0.5, 1.5, 0.5),
                10f,
                10f,
                0.2f,
                30,
                1,
                0f,
                90f,
                90f,
                Color(0, 255, 0, 85),
                true, false
            )
        }
    }
}
