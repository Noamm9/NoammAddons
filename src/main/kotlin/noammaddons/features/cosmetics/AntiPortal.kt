package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.mixins.EntityAccessor
import noammaddons.utils.LocationUtils
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
