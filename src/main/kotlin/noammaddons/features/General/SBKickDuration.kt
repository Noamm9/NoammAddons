package noammaddons.features.General

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.sounds.notificationsound
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import noammaddons.utils.ThreadUtils.setTimeout
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.CustomFont.getTextWidth

object SBKickDuration {
	private var showTime = false
	private var lastKickTime = System.currentTimeMillis()
	private var hasWarned = false
	
	@SubscribeEvent
	fun onChat(event: Chat) {
		 if (!config.SBKickDuration) return
		
		if (event.component.unformattedText.removeFormatting().equalsOneOf(
			"You were kicked while joining that server!",
			"There was a problem joining SkyBlock, try again in a moment!"
		)) {
			showTime = true
			lastKickTime = System.currentTimeMillis()
		}
	}
	
	
	
	@SubscribeEvent
	fun onRenderOverlay(event: RenderOverlay) {
		 if (!config.SBKickDuration) return
		if (!onHypixel || !showTime || inSkyblock) return
		val timeSinceKick = System.currentTimeMillis() - lastKickTime
		
		// If more than 1 minute passed, show warning and play sound
		if (timeSinceKick >= 60_000) {
			if (hasWarned || inSkyblock) return
			
			hasWarned = true
			showTitle("§eTry rejoining SkyBlock now!")
			playNotificationSound()
			showTime = false
			return
		}
		else {
			val format = "§cLast kicked from SkyBlock §b${String.format("%.2f", timeSinceKick / 1000.0)}s ago"
			val scaledWidth = mc.getWidth() / 2 - (getTextWidth(format) * 1.5f / 2f)
			val scaledHeight = (mc.getHeight() / 2f - 20)
			
			drawText(format, scaledWidth, scaledHeight, 1f)
		}
	}
	
	private fun playNotificationSound() {
		notificationsound.play()
		setTimeout(500) { notificationsound.play() }
		setTimeout(1000) { notificationsound.play() }
	}
}
