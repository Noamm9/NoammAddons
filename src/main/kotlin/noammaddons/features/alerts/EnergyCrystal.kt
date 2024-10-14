package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth

object EnergyCrystal {
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun title(event: RenderOverlay) {
	    if (!config.energyCrystalAlert) return
		if (Player?.inventory?.mainInventory
		  ?.get(8)?.displayName?.removeFormatting() != "Energy Crystal"
		) return

	    drawCenteredText("&e&l⚠ &l&bCrystal &e&l⚠", mc.getWidth()/ 2f, mc.getHeight()*0.4f, 4.5f)
    }
}