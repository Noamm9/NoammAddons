package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.noammaddons.Companion.mc
import noammaddons.config.EditGui.HudElement
import noammaddons.events.PacketEvent
import noammaddons.events.RenderOverlay
import noammaddons.sounds.potispow
import noammaddons.utils.ItemUtils.SkyblockID
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object WitherShieldTimer {

    private val WitherShieldElement = HudElement("&e&l5", dataObj = hudData.getData().WitherShieldTimer)
    private var tickTimer = 101
    private val WitherBlades = listOf(
        "ASTRAEA",
        "HYPERION",
        "VALKYRIE",
        "SCYLLA"
    )

    @SubscribeEvent
    fun onServerTick(event: PacketEvent.Received) {
        if (!config.WitherShieldTimer) return
        if (event.packet !is S32PacketConfirmTransaction) return
        if (tickTimer > 100) return

        tickTimer += 1

        if (tickTimer == 100) potispow.play()

    }

    @SubscribeEvent
    fun onSentRightClick(event: PacketEvent.Sent) {
        if (!config.WitherShieldTimer) return
        if (event.packet !is C08PacketPlayerBlockPlacement) return
        if (tickTimer < 100) return

        if (WitherBlades.contains(mc.thePlayer.heldItem?.SkyblockID)) {
            tickTimer = 0
        }
    }

    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (!config.WitherShieldTimer) return
        if (tickTimer >= 100) return // Only display when the timer is running

        WitherShieldElement
            .setText((((5000 - tickTimer*50) / 100) / 10.0).toString())
            .setColor(when {
                tickTimer < 33 -> Color.RED
                tickTimer < 66 -> Color.YELLOW
                else -> Color.GREEN
            })
            .draw()
    }
}
