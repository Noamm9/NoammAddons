package noammaddons.features.misc

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.mixins.EntityAccessor
import noammaddons.utils.LocationUtils.inSkyblock

object AntiPortal: Feature() {
    @SubscribeEvent
    fun antiPortal(event: RenderWorld) {
        if (! config.antiPortal || ! inSkyblock) return
        (mc.thePlayer as EntityAccessor).run {
            if (! isInPortal) return

            setInPortal(false)
        }
    }
}
