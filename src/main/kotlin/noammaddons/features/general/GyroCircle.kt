package noammaddons.features.general

import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.BlockUtils.toVec3
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawCylinder


object GyroCircle: Feature() {
    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! config.ShowGyroCircle) return
        if (Player?.heldItem?.SkyblockID != "GYROKINETIC_WAND") return
        val Blockpos = Player !!.rayTrace(25.0, event.partialTicks)?.blockPos ?: return
        val BlockIdAtPos = getBlockAt(Blockpos)?.getBlockId()
        val BlockIdAbovePos = getBlockAt(Blockpos.add(.0, 1.0, .0))?.getBlockId()

        if (BlockIdAtPos == 0 || (BlockIdAbovePos != 0 && BlockIdAbovePos != 171)) return

        drawBlockBox(
            Blockpos,
            config.ShowGyroCircleBlockColor,
            outline = true, fill = true,
            phase = true
        )

        drawCylinder(
            Blockpos.toVec3().add(Vec3(0.5, 1.5, 0.5)),
            10.0, config.ShowGyroCircleRingColor
        )
    }
}
