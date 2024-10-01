package noammaddons.features.hud

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.config.EditGui.HudElement
import noammaddons.events.RenderOverlay
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ClockDisplay {
    private val ClockDisplay = HudElement(
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
        config.ClockDisplayColor, hudData.getData().ClockDisplay
    )

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun draw(event: RenderOverlay) {
        if (!config.ClockDisplay) return

        ClockDisplay
        .setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
        .setColor(config.ClockDisplayColor)
        .draw()
    }
}
