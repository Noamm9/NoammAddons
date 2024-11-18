package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.components.TextElement
import noammaddons.events.ClickEvent.RightClickEvent
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.features.Feature
import noammaddons.utils.PlayerUtils.isHoldingWitherImpact
import noammaddons.utils.SoundUtils.potisPow
import java.awt.Color


object WitherShieldTimer: Feature() {
    private val WitherShieldElement = TextElement("&e&l5", dataObj = hudData.getData().WitherShieldTimer)
    private var tickTimer = 101


    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! config.WitherShieldTimer) return
        if (tickTimer > 100) return

        tickTimer += 1

        if (tickTimer == 100) potisPow.start()
    }

    @SubscribeEvent
    fun onSentRightClick(event: RightClickEvent) {
        if (! config.WitherShieldTimer) return
        if (tickTimer < 100) return
        if (! isHoldingWitherImpact()) return

        tickTimer = 0
    }

    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (! config.WitherShieldTimer) return
        if (tickTimer >= 100) return // Only display when the timer is running

        WitherShieldElement.run {
            setText((((5000 - tickTimer * 50) / 100) / 10.0).toString())
            setColor(
                when {
                    tickTimer < 33 -> Color.RED
                    tickTimer < 66 -> Color.YELLOW
                    else -> Color.GREEN
                }
            )
            draw()
        }
    }
}
