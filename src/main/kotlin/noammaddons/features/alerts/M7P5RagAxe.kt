package noammaddons.features.alerts

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.SoundUtils.iHaveNothing
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.RenderUtils.drawCenteredChromaWaveText
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
	    
	    
	    setTimeout(1600) {
		    iHaveNothing.start()
		    setTimeout(400) {
			    showTitle = true
			    setTimeout(3500) {
					showTitle = false
			    }
		    }
		}
    }
	
	@SubscribeEvent
	@Suppress("UNUSED_PARAMETER")
	fun draw(e: RenderOverlay) {
		if (!showTitle) return
		
		drawCenteredChromaWaveText(
			"rag",
			mc.getWidth()/2f,
			mc.getHeight()/2f - 70f,
			scale = 4f,
			waveSpeed = 2f
		)
	}
}