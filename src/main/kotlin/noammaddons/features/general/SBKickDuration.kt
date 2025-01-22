package noammaddons.features.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.SoundUtils.notificationSound
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf


object SBKickDuration: Feature() {
    private var showTime = false
    private var lastKickTime = System.currentTimeMillis()
    private var hasWarned = false

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.SBKickDuration) return

        if (event.component.noFormatText.equalsOneOf(
                "You were kicked while joining that server!",
                "There was a problem joining SkyBlock, try again in a moment!"
            )
        ) {
            showTime = true
            lastKickTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! config.SBKickDuration) return
        if (! onHypixel || ! showTime || inSkyblock) return
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
            drawCenteredText(
                "§cLast kicked from SkyBlock §b${String.format("%.2f", timeSinceKick / 1000.0)}s ago",
                mc.getWidth() / 2f,
                mc.getHeight() / 2f - 20,
                1.5f
            )
        }
    }

    private fun playNotificationSound() {
        notificationSound.start()
        setTimeout(500) { notificationSound.start() }
        setTimeout(1000) { notificationSound.start() }
    }
}
