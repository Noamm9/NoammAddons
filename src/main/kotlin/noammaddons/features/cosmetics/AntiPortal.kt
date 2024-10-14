package noammaddons.features.cosmetics

import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.mixins.EntityAccessor
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PlayerUtils.Player

object AntiPortal {
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun antiPortal(event: RenderWorldLastEvent) {
        if (!config.antiPortal || !inSkyblock) return
        (Player as EntityAccessor).run {
            if (!isInPortal) return

            setInPortal(false)
        }
    }
}
