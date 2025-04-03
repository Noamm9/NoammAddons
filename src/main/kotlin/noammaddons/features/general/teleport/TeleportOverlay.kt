package noammaddons.features.general.teleport

import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.features.general.teleport.ZeroPingTeleportation.TeleportInfo
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.PlayerUtils
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ServerPlayer
import noammaddons.utils.Utils.equalsOneOf

object TeleportOverlay: Feature() {

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! config.teleportOverlay) return

        val heldItem = mc.thePlayer.heldItem ?: return
        val teleportInfo = getType(heldItem) ?: return
        val playerPos = ServerPlayer.player.getVec()
        val playerRot = ServerPlayer.player.getRotation()

        if (teleportInfo.type == TeleportInfo.Companion.Types.Etherwarp) {
            if (! config.teleportOverlayEtherwarp || ! ServerPlayer.player.sneaking) return
            val (valid, pos) = EtherwarpHelper.getEtherPos(playerPos, playerRot, teleportInfo.distance)
            drawBlockBox(
                blockPos = pos ?: return,
                overlayColor = if (valid) config.teleportOverlayOverlayColor else config.etherwarpOverlayOverlayColorInvalid,
                outlineColor = if (valid) config.teleportOverlayOutlineColor else config.etherwarpOverlayOverlayColorInvalid,
                outline = config.teleportOverlayType.equalsOneOf(0, 2),
                fill = config.teleportOverlayType.equalsOneOf(1, 2),
                phase = config.TeleportOverlayESP,
                LineThickness = config.teleportOverlayOutlineThickness
            )
            return
        }

        if ((teleportInfo.type == TeleportInfo.Companion.Types.InstantTransmission && ! config.teleportOverlayInstantTransmission) ||
            (teleportInfo.type == TeleportInfo.Companion.Types.WitherImpact && ! config.teleportOverlayWitherImpact) ||
            (PlayerUtils.isHoldingEtherwarpItem(heldItem) && ServerPlayer.player.sneaking)
        ) return

        val prediction = InstantTransmissionPredictor.predictTeleport(
            teleportInfo.distance,
            playerPos.xCoord,
            playerPos.yCoord,
            playerPos.zCoord,
            playerRot.yaw.toDouble(),
            playerRot.pitch.toDouble()
        ) ?: return

        drawBlockBox(
            blockPos = BlockPos(prediction),
            overlayColor = config.teleportOverlayOverlayColor,
            outlineColor = config.teleportOverlayOutlineColor,
            outline = config.teleportOverlayType.equalsOneOf(0, 2),
            fill = config.teleportOverlayType.equalsOneOf(1, 2),
            phase = config.TeleportOverlayESP,
            LineThickness = config.teleportOverlayOutlineThickness
        )
    }

    fun getType(stack: ItemStack): TeleportInfo? {
        if (stack.SkyblockID.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val nbt = stack.getSubCompound("ExtraAttributes", false)
            val tuners = nbt?.getByte("tuned_transmission")?.toInt() ?: 0
            return if (ServerPlayer.player.sneaking && nbt?.getByte("ethermerge") == 1.toByte()) {
                TeleportInfo(57.0 + tuners - 1, TeleportInfo.Companion.Types.Etherwarp)
            }
            else {
                TeleportInfo(8.0 + tuners, TeleportInfo.Companion.Types.InstantTransmission)
            }
        }
        if (PlayerUtils.isHoldingWitherImpact(stack)) {
            return TeleportInfo(10.0, TeleportInfo.Companion.Types.WitherImpact)
        }
        return null
    }
}
