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


object BonzoMask {
    private val BonzoMaskElement = HudElement("&9Bonzo Mask: &aREADY", dataObj = hudData.getData().BonzoMask)
    private var timer = 0L
    private var draw = false
    private val maskCooldown = 183_000

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (!config.BonzoMaskDisplay) return
	    
        if (!event.component.unformattedText.removeFormatting().matches(Regex("^Your (?:. )?Bonzo's Mask saved your life!$"))) return
        timer = System.currentTimeMillis()
        draw = true
	    
	    if (!config.BonzoMaskAlert) return
	    showTitle("&9Bonzo Mask")
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (!config.BonzoMaskDisplay) return
        if (!draw) return

        val cooldown = ((maskCooldown + (timer - System.currentTimeMillis())).toDouble()/1000).toFixed(1).toDouble()

        BonzoMaskElement.setText(
            when {
                cooldown > 0.0 -> "&9Bonzo Mask: &a$cooldown"
                (cooldown == 0.0 || cooldown > -30.0) -> "&9Bonzo Mask: &aREADY"
                else -> return
            }
        ).draw()
    }
}
