package noammaddons.features.alerts

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.RenderOverlay
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EnergyCrystal {
    @SubscribeEvent
    fun title(event: RenderOverlay) {
	    if (!config.energyCrystalAlert) return
		if (mc.thePlayer?.inventory?.mainInventory
		  ?.get(8)?.displayName?.removeFormatting() != "Energy Crystal"
		) return

	    drawCenteredText("&e&l⚠ &l&bCrystal &e&l⚠", mc.getWidth()/ 2.0, mc.getHeight()*0.4, 4.5)
    }
}