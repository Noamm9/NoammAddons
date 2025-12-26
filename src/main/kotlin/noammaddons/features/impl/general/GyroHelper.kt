package noammaddons.features.impl.general

import gg.essential.elementa.utils.withAlpha
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.getBlockFromLook
import noammaddons.utils.PlayerUtils
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawCylinder
import noammaddons.utils.ServerPlayer.player
import noammaddons.utils.Utils.favoriteColor


object GyroHelper: Feature("Shows the sucking range of the Gyrokinetic Wand") {
    private val useServerPos = ToggleSetting("Use Server Position", false)
    private val drawRing = ToggleSetting("Ring", true)
    private val drawBox = ToggleSetting("Box")

    private val boxColor = ColorSetting("Box Color", favoriteColor.withAlpha(0.3f))
    private val ringColor = ColorSetting("Ring Color", favoriteColor)

    override fun init() = addSettings(
        useServerPos, drawRing, drawBox,
        SeperatorSetting("Colors"),
        boxColor, ringColor
    )

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! drawRing.value && ! drawBox.value) return
        if (boxColor.value.alpha + ringColor.value.alpha == 0) return
        if (mc.thePlayer?.heldItem?.skyblockID != "GYROKINETIC_WAND") return
        val rot = player.getRotation().takeIf { useServerPos.value } ?: PlayerUtils.getRotation()
        val pos = player.getVec()?.add(y = mc.thePlayer.getEyeHeight()).takeIf { useServerPos.value } ?: PlayerUtils.getEyePos()
        val gyroPos = getBlockFromLook(rot, 25, pos.xCoord, pos.yCoord, pos.zCoord) ?: return
        val posID = getBlockAt(gyroPos).getBlockId()
        val idAbovePos = getBlockAt(gyroPos.add(y = 1)).getBlockId()

        if (posID == 0 || (idAbovePos != 0 && idAbovePos != 171)) return

        if (drawBox.value) drawBlockBox(
            gyroPos, boxColor.value,
            outline = true, fill = true,
        )

        if (drawRing.value) drawCylinder(
            Vec3(gyroPos).add(0.5, 1.5, 0.5),
            10.0, ringColor.value
        )
    }
}
