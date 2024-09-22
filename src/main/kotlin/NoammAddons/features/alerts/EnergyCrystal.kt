package NoammAddons.features.alerts

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.RenderOverlay
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.RenderUtils.drawCenteredText
import NoammAddons.utils.RenderUtils.drawText
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EnergyCrystal {
	private val showEnergyCrystal get() = mc.thePlayer?.inventory?.mainInventory?.get(8)?.displayName?.removeFormatting() == "Energy Crystal"
    private val text = "&e&l⚠ &l&bCrystal &e&l⚠ ".addColor()

	
    @SubscribeEvent
    fun title(event: RenderOverlay) {
		if (!showEnergyCrystal) return
	    if (!config.energyCrystalAlert) return

	    drawCenteredText(text, mc.getWidth()/ 2.0, mc.getHeight() / 2 - 20.0, 4.5)
    }
}