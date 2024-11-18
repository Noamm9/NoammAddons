package noammaddons.features.misc

import net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.Utils.equalsOneOf

object BlockOverlay: Feature() {
    @SubscribeEvent
    fun drawBlockHighlight(event: DrawBlockHighlightEvent) {
        event.isCanceled = config.BlockOverlay
    }

    @SubscribeEvent
    fun drawBlockOverlay(event: RenderWorld) {
        if (! config.BlockOverlay) return
        if (mc.gameSettings.hideGUI) return
        mc.objectMouseOver?.run {
            if (typeOfHit != BLOCK) return

            drawBlockBox(
                blockPos = blockPos,
                overlayColor = config.BlockOverlayOverlayColor,
                outlineColor = config.BlockOverlayOutlineColor,
                outline = config.BlockOverlayType.equalsOneOf(0, 2),
                fill = config.BlockOverlayType.equalsOneOf(1, 2),
                phase = config.BlockOverlayESP,
                LineThickness = config.BlockOverlayOutlineThickness
            )
        }
    }
}