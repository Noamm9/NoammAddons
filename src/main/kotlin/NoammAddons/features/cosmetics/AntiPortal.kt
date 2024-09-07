package NoammAddons.features.cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.mixins.EntityAccessor
import NoammAddons.utils.LocationUtils
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AntiPortal {
    @SubscribeEvent
    fun antiPortal(event: RenderWorldLastEvent) {
        if (config.antiPortal && LocationUtils.inSkyblock) {
            (mc.thePlayer as EntityAccessor).setInPortal(false)
        }
    }
}
