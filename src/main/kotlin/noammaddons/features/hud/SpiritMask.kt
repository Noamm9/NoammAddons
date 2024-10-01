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

object SpiritMask {
    private val SpiritMaskElement = HudElement("&fSpirit Mask: &aREADY", dataObj = hudData.getData().SpiritMask)
    private val maskCooldown = 30_000
    private var timer = 0L
    private var draw = false

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (!config.SpiritMaskDisplay) return
        if (!event.component.unformattedText.removeFormatting().matches(
			Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")
		)) return
     
	    timer = System.currentTimeMillis()
        draw = true
	    
	    if (!config.SpiritMaskAlert) return
	    showTitle("&fSpirit Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (!config.SpiritMaskDisplay) return
        if (!draw) return

        val cooldown = ((maskCooldown + (timer - System.currentTimeMillis())).toDouble() / 1000).toFixed(1).toDouble()

        SpiritMaskElement.setText(
            when {
                cooldown > 0.0 -> "&fSpirit Mask: &a$cooldown"
                (cooldown == 0.0 || cooldown > -30.0) -> "&fSpirit Mask: &aREADY"
                else -> return
            }
        ).draw()
    }
}
