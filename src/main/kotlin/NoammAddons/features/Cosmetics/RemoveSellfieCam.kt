package NoammAddons.features.Cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object RemoveSellfieCam {
    @SubscribeEvent
    fun RenderGameOverlay(event: RenderGameOverlayEvent.Pre) {
        if (!config.removeSelfieCamera) return

        if (mc.gameSettings.thirdPersonView == 2) mc.gameSettings.thirdPersonView = 0
    }
}