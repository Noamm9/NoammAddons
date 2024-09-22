package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.config.EditGui.HudElement
import NoammAddons.events.RenderOverlay
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
