package noammaddons.features.hud

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.hudData
import noammaddons.config.EditGui.HudElement
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.MathUtils.toFixed
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
