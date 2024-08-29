package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.events.RenderLivingEntityEvent
import NoammAddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import NoammAddons.utils.LocationUtils.inSkyblock
import NoammAddons.utils.OutlineUtils.outlineESP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object TeammatesOutline {
    @SubscribeEvent
    fun renderOutline(event: RenderLivingEntityEvent) {
        if (!inSkyblock || !config.dungeonTeammatesOutline) return
        dungeonTeammatesNoSelf.forEach{
            if (event.entity == it.entity) outlineESP(event, it.clazz.color)
        }
    }
}
