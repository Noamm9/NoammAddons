package NoammAddons.features.alerts

import NoammAddons.NoammAddons.Companion.MOD_ID
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.Chat
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.showTitle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object RNGSound {
    private val regex = Regex("^&d&lRNG METER! &r&aReselected the (.+?) &afor .+ &e&lCLICK HERE &r&ato select a new drop!&r$".addColor())
    // RNG METER! Reselected the Tarantula Talisman for Spider Slayer! CLICK HERE to select a new drop!

    @SubscribeEvent
    fun onChat(event: Chat) {
		if (!config.RNGSound) return
        val msg = event.component.formattedText
		val rngDrop = regex.find(msg)?.destructured?.component1() ?: return
	    
	    showTitle(rngDrop)
	    mc.thePlayer.playSound("$MOD_ID:chipi_chapa", 1f, 1f)
    }
}