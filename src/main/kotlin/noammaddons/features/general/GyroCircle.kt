package noammaddons.features.general

import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.MathUtils
import noammaddons.utils.MathUtils.add
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawCylinder
import noammaddons.utils.ServerPlayer


object GyroCircle: Feature() {
    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! config.ShowGyroCircle) return
        if (mc.thePlayer?.heldItem?.SkyblockID != "GYROKINETIC_WAND") return
        val serverRot = ServerPlayer.player.getRotation()
        val serverPos = ServerPlayer.player.getVec().add(y = mc.thePlayer.getEyeHeight())
        val pos = MathUtils.getBlockFromLook(serverRot, 25, serverPos.xCoord, serverPos.yCoord, serverPos.zCoord) ?: return
        val posID = getBlockAt(pos).getBlockId()
        val idAbovePos = getBlockAt(pos.add(y = 1)).getBlockId()

        if (posID == 0 || (idAbovePos != 0 && idAbovePos != 171)) return

        drawBlockBox(
            pos,
            config.ShowGyroCircleBlockColor,
            outline = true, fill = true,
        )

        drawCylinder(
            Vec3(pos).add(0.5, 1.5, 0.5),
            10.0, config.ShowGyroCircleRingColor
        )
    }
}
