package noammaddons.features.hud

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.config.EditGui.HudElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.ChatUtils.toFixed
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
