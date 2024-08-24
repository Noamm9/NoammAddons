package NoammAddons.features.Cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.RenderUtils.drawBlockBox
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BlockOverlay {

    @SubscribeEvent
    fun DrawBlockOverlay(event: DrawBlockHighlightEvent) {
        if (event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && config.BlockOverlay) {
            event.isCanceled = true
            val blockpos = mc.objectMouseOver?.blockPos

            if (blockpos != null) {
                when (config.BlockOverlayType) {

                    0 -> drawBlockBox(blockpos, config.BlockOverlayOutlineColor, true, false, config.BlockOverlayESP, config.BlockOverlayOutlineThickness)

                    1 -> drawBlockBox(blockpos, config.BlockOverlayOverlayColor, false, true, config.BlockOverlayESP, config.BlockOverlayOutlineThickness)

                    else -> {
                        drawBlockBox(blockpos, config.BlockOverlayOutlineColor, true, false, config.BlockOverlayESP, config.BlockOverlayOutlineThickness)
                        drawBlockBox(blockpos, config.BlockOverlayOverlayColor, false, true, config.BlockOverlayESP, config.BlockOverlayOutlineThickness)
                    }
                }
            }
        }
    }
}