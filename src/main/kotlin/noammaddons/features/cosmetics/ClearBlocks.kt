package noammaddons.features.cosmetics

import net.minecraftforge.client.event.RenderBlockOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config

object ClearBlocks {
	@SubscribeEvent
	fun removeOverlay(event: RenderBlockOverlayEvent) {
		event.isCanceled = config.clearBlocks
	}
}
