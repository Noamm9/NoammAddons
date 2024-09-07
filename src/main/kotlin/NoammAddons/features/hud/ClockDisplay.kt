package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.config.EditGui.HudElement
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ClockDisplay {
    private val ClockDisplay = HudElement(
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
        config.ClockDisplayColor, hudData.getData().ClockDisplay
    )

    @SubscribeEvent
    fun draw(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        if (!config.ClockDisplay) return

        ClockDisplay
        .setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
        .setColor(config.ClockDisplayColor)
        .draw()
    }
}
