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
