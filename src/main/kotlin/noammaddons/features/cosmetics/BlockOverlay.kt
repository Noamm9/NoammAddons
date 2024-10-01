package noammaddons.features.cosmetics

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.RenderUtils.drawBlockBox
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BlockOverlay {
    @SubscribeEvent
    fun drawBlockOverlay(event: DrawBlockHighlightEvent) {
        if (event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && config.BlockOverlay) {
            event.isCanceled = true
            val blockpos = mc.objectMouseOver?.blockPos ?: return

            when (config.BlockOverlayType) {
                0 -> drawBlockBox(
                    blockpos,
                    config.BlockOverlayOutlineColor,
                    outline = true, fill = false,
                    phase = config.BlockOverlayESP,
                    LineThickness = config.BlockOverlayOutlineThickness
                )

                1 -> drawBlockBox(
                    blockpos,
                    config.BlockOverlayOverlayColor,
                    outline = false, fill = true,
                    phase = config.BlockOverlayESP,
                    LineThickness = config.BlockOverlayOutlineThickness
                )

                else -> {
                    drawBlockBox(
                        blockpos,
                        config.BlockOverlayOutlineColor,
                        outline = true, fill = false,
                        phase = config.BlockOverlayESP,
                        LineThickness = config.BlockOverlayOutlineThickness
                    )
                    drawBlockBox(
                        blockpos,
                        config.BlockOverlayOverlayColor,
                        outline = false, fill = true,
                        phase = config.BlockOverlayESP,
                        LineThickness = config.BlockOverlayOutlineThickness
                    )
                }
            }
        }
    }
}