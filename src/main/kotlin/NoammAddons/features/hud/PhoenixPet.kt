package NoammAddons.features.hud

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.config.EditGui.HudElement
import NoammAddons.events.Chat
import NoammAddons.events.RenderOverlay
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.showTitle
import NoammAddons.utils.ChatUtils.toFixed
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object PhoenixPet {
    private val PhoenixPetElement = HudElement("&5Phoenix Pet: &aREADY", dataObj = hudData.getData().PhoenixPet)
    private var timer = 0L
    private var draw = false
    private val petCooldown = 60_000

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (!config.PhoenixPetDisplay) return
        if (!event.component.unformattedText.removeFormatting().matches(
			Regex("^Your Phoenix Pet saved you from certain death!$")
		)) return
     
	    timer = System.currentTimeMillis()
        draw = true
	    
	    if (!config.PhoenixPetAlert) return
	    showTitle("&5Phoenix Pet")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (!config.PhoenixPetDisplay) return
        if (!draw) return

        val cooldown = ((petCooldown + (timer - System.currentTimeMillis())).toDouble() / 1000).toFixed(1).toDouble()

        PhoenixPetElement.setText(
            when {
                cooldown > 0.0 -> "&5Phoenix Pet: &a$cooldown"
                (cooldown == 0.0 || cooldown > -30.0) -> "&5Phoenix Pet: &aREADY"
                else -> return
            }
        ).draw()
    }
}
