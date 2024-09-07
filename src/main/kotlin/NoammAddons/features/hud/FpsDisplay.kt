package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.config.EditGui.HudElement
import NoammAddons.utils.Utils.getFPS
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FpsDisplay {
    private val FpsDisplayElement = HudElement("${mc.getFPS()} fps", config.FpsDisplayColor, hudData.getData().FPSdisplay)

    @SubscribeEvent
    fun draw(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        if (!config.FpsDisplay) return

        FpsDisplayElement
        .setText("${mc.getFPS()} fps")
        .setColor(config.FpsDisplayColor)
        .draw()
    }
}
