package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.Chat
import NoammAddons.events.RenderOverlay
import NoammAddons.sounds.notificationsound
import NoammAddons.utils.ChatUtils.equalsOneOf
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ChatUtils.showTitle
import NoammAddons.utils.LocationUtils.inSkyblock
import NoammAddons.utils.LocationUtils.onHypixel
import NoammAddons.utils.RenderUtils.drawText
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import NoammAddons.utils.ThreadUtils.setTimeout
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

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
			val scaledWidth = mc.getWidth() / 2 - (mc.fontRendererObj.getStringWidth(format) * 1.5 / 2)
			val scaledHeight = (mc.getHeight() / 2 - 20).toDouble()
			
			drawText(format, scaledWidth, scaledHeight, 1.5)
		}
	}
	
	private fun playNotificationSound() {
		notificationsound.play()
		setTimeout(500) { notificationsound.play() }
		setTimeout(1000) { notificationsound.play() }
	}
}
