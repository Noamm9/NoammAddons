package NoammAddons.features.cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.client.event.EntityViewRenderEvent


object CustomFov {
    @SubscribeEvent
    fun onFOVModifier(event: EntityViewRenderEvent.FOVModifier) {
        if (!config.CustomFov) return
        mc.gameSettings.fovSetting = config.CustomFovValue
    }
}