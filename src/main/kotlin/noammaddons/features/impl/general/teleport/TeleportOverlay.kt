package noammaddons.features.impl.general.teleport

import gg.essential.elementa.utils.withAlpha
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.features.impl.general.teleport.ZeroPingTeleportation.TeleportInfo
import noammaddons.features.impl.general.teleport.ZeroPingTeleportation.TeleportInfo.Companion.Types
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.PlayerUtils
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.ServerPlayer
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.favoriteColor
import java.awt.Color

object TeleportOverlay: Feature() {

    private val aote = ToggleSetting("Instant Transmission")
    private val etherwarp = ToggleSetting("Etherwarp")
    private val witherImpact = ToggleSetting("Wither Impact")

    private val mode = DropdownSetting("Mode", listOf("Outline", "Fill", "Filled Outline"))
    private val phase = ToggleSetting("Phase")
    private val lineWidth = SliderSetting("Line Width", 1, 10, 1, 2).addDependency { mode.value == 1 }

    private val fillColor = ColorSetting("Fill Color", favoriteColor.withAlpha(50)).addDependency { mode.value == 0 }
    private val outlineColor = ColorSetting("Outline Color", favoriteColor, false).addDependency { mode.value == 1 }

    private val invalidFillColor = ColorSetting("Fill Color ", Color.RED.withAlpha(50)).addDependency { mode.value == 0 }.addDependency(etherwarp)
    private val invalidOutlineColor = ColorSetting("Outline Color ", Color.RED, false).addDependency { mode.value == 1 }.addDependency(etherwarp)

    override fun init() = addSettings(
        aote, etherwarp, witherImpact,
        SeperatorSetting("Settings"),
        mode, phase, lineWidth,
        SeperatorSetting("Colors"),
        fillColor, outlineColor,
        SeperatorSetting("Invalid Colors").addDependency(etherwarp),
        invalidFillColor, invalidOutlineColor
    )

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        val heldItem = ServerPlayer.player.getHeldItem() ?: return
        val teleportInfo = getType(heldItem) ?: return
        val playerPos = ServerPlayer.player.getVec()
        val playerRot = ServerPlayer.player.getRotation()

        if (teleportInfo.type == Types.Etherwarp) {
            if (! etherwarp.value || ! ServerPlayer.player.sneaking) return
            val (valid, pos) = EtherwarpHelper.getEtherPos(playerPos, playerRot, teleportInfo.distance)
            drawBlockBox(
                blockPos = pos ?: return,
                overlayColor = if (valid) fillColor.value else invalidFillColor.value,
                outlineColor = if (valid) outlineColor.value else invalidOutlineColor.value,
                outline = mode.value.equalsOneOf(0, 2),
                fill = mode.value.equalsOneOf(1, 2),
                phase = phase.value,
                LineThickness = lineWidth.value.toFloat()
            )
            return
        }

        if ((teleportInfo.type == Types.InstantTransmission && ! aote.value) ||
            (teleportInfo.type == Types.WitherImpact && ! witherImpact.value) ||
            (PlayerUtils.isHoldingEtherwarpItem(heldItem) && ServerPlayer.player.sneaking)
        ) return

        val prediction = InstantTransmissionPredictor.predictTeleport(teleportInfo.distance, playerPos, playerRot) ?: return


        drawBox(
            prediction.xCoord - .25,
            prediction.yCoord + 0.25,
            prediction.zCoord - .25,
            color = fillColor.value,
            outline = mode.value.equalsOneOf(0, 2),
            fill = mode.value.equalsOneOf(1, 2),
            width = 0.5, height = 0.5,
            phase = phase.value,
            lineWidth = lineWidth.value.toFloat()
        )
    }

    fun getType(stack: ItemStack): TeleportInfo? {
        if (stack.skyblockID.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val nbt = stack.getSubCompound("ExtraAttributes", false)
            val tuners = nbt?.getByte("tuned_transmission")?.toInt() ?: 0
            return if (ServerPlayer.player.sneaking && nbt?.getByte("ethermerge") == 1.toByte()) {
                TeleportInfo(57.0 + tuners - 1, Types.Etherwarp)
            }
            else {
                TeleportInfo(8.0 + tuners, Types.InstantTransmission)
            }
        }
        if (PlayerUtils.isHoldingWitherImpact(stack)) {
            return TeleportInfo(10.0, Types.WitherImpact)
        }
        return null
    }
}
