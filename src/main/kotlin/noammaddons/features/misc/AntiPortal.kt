package noammaddons.features.misc

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.mixins.EntityAccessor
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.PlayerUtils.Player

object AntiPortal: Feature() {
    @SubscribeEvent
    fun antiPortal(event: RenderWorld) {
        if (! config.antiPortal || ! inSkyblock) return
        (Player as EntityAccessor).run {
            if (! isInPortal) return

            setInPortal(false)
        }
    }
}
