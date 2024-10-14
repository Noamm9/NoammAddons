package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.RenderUtils
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.PlayerUtils.Player
import java.awt.Color


object GyroCircle {
    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!config.ShowGyroCircle) return
        if (Player?.heldItem?.SkyblockID != "GYROKINETIC_WAND") return

        val Blockpos = Player!!.rayTrace(25.0, event.partialTicks)?.blockPos ?: return
        val BlockIdAtPos = mc.theWorld.getBlockAt(Blockpos).getBlockId()
        val BlockIdAbovePos = mc.theWorld.getBlockAt(Blockpos.add(.0, 1.0, .0)).getBlockId()

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
	            phase = true,
	            linemode = false
            )
        }
    }
}
