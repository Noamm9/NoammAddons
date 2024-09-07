package NoammAddons.features.cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object RemoveSellfieCam {
    @SubscribeEvent
    fun removeSelfieCamera(event: TickEvent.ClientTickEvent) {
        if (!config.removeSelfieCamera) return
        if (mc.gameSettings.keyBindTogglePerspective.isKeyDown || mc.gameSettings.keyBindTogglePerspective.isPressed) {
            if (mc.gameSettings.thirdPersonView == 2) mc.gameSettings.thirdPersonView = 0
        }
    }
}