package noammaddons.features.General

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.HudElement
import noammaddons.events.ClickEvent.RightClickEvent
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.utils.PlayerUtils.isHoldingWitherImpact
import noammaddons.utils.SoundUtils.potisPow
import java.awt.Color


object WitherShieldTimer {
    private val WitherShieldElement = HudElement("&e&l5", dataObj = hudData.getData().WitherShieldTimer)
    private var tickTimer = 101


    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onServerTick(event: ServerTick) {
        if (!config.WitherShieldTimer) return
        if (tickTimer > 100) return

        tickTimer += 1

        if (tickTimer == 100) potisPow.start()
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onSentRightClick(event: RightClickEvent) {
        if (!config.WitherShieldTimer) return
        if (tickTimer < 100) return
        if (!isHoldingWitherImpact()) return
	    
	    tickTimer = 0
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
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
