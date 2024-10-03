package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.sounds.ihavenothing
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.CustomFont
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.RenderUtils.drawCenteredChromaWaveText
import noammaddons.utils.RenderUtils.drawChromaWaveText
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import noammaddons.utils.ThreadUtils.setTimeout

object M7P5RagAxe {
	private var showTitle: Boolean = false
	
    @SubscribeEvent
    fun onChat(event: Chat) {
		if (!config.M7P5RagAxe) return
        if (F7Phase != 5) return
	    if (event.component.unformattedText.removeFormatting() != "[BOSS] Wither King: You... again?") return
	    
	    setTimeout(2000) {
			showTitle = true
		    ihavenothing.play()
		}
	    setTimeout(2000 + 3500) { showTitle = false }
	    debugMessage("Rag!")
    }
	
	@SubscribeEvent
	@Suppress("UNUSED_PARAMETER")
	fun drawText(event: RenderOverlay) {
		if (!showTitle) return
		
		drawCenteredChromaWaveText(
			"rag",
			mc.getWidth()/2f,
			mc.getHeight()/2f - 70f,
			scale = 4f
		)
	}
}