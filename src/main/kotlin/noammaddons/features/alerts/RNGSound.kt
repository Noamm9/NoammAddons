package noammaddons.features.alerts

import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.showTitle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.SoundUtils.chipiChapa

object RNGSound {
    private val regex = Regex("^&d&lRNG METER! &r&aReselected the (.+?) &afor .+ &e&lCLICK HERE &r&ato select a new drop!&r$".addColor())
    // RNG METER! Reselected the Tarantula Talisman for Spider Slayer! CLICK HERE to select a new drop!

    @SubscribeEvent
    fun onChat(event: Chat) {
		if (!config.RNGSound) return
        val msg = event.component.formattedText
		val rngDrop = regex.find(msg)?.destructured?.component1() ?: return
	    
	    showTitle(subtitle = rngDrop)
	    chipiChapa.start()
    }
}