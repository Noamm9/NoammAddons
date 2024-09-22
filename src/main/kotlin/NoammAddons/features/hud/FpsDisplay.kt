package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.config.EditGui.HudElement
import NoammAddons.events.RenderOverlay
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FpsDisplay {
    private val FpsDisplayElement = HudElement("${Minecraft.getDebugFPS()} fps", config.FpsDisplayColor, hudData.getData().FPSdisplay)

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun draw(event: RenderOverlay) {
        if (!config.FpsDisplay) return

        FpsDisplayElement
        .setText("${Minecraft.getDebugFPS()} fps")
        .setColor(config.FpsDisplayColor)
        .draw()
    }
}
