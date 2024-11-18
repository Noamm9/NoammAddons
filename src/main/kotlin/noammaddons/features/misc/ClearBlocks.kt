package noammaddons.features.misc

import net.minecraftforge.client.event.RenderBlockOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature

object ClearBlocks : Feature() {
    @SubscribeEvent
    fun removeOverlay(event: RenderBlockOverlayEvent) {
        event.isCanceled = config.clearBlocks
    }
}
