package noammaddons.features.impl.general

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout


object SBKick: Feature("Shows how long it has been since the last time you were kicked from SkyBlock") {
    private val timer by ToggleSetting("Show Timer")
    private val sendMsg by ToggleSetting("Send Party Message")


    private var showTime = false
    private var lastKickTime = System.currentTimeMillis()
    private var hasWarned = false

    @SubscribeEvent
    fun onChat(event: Chat) {
        when (event.component.noFormatText) {
            "There was a problem joining SkyBlock, try again in a moment!" -> {
                lastKickTime = System.currentTimeMillis()
                showTime = true
            }

            "You were kicked while joining that server!" -> {
                if (sendMsg) sendPartyMessage("$CHAT_PREFIX You were kicked while joining that server!")
                lastKickTime = System.currentTimeMillis()
                showTime = true
            }

            "A kick occurred in your connection, so you were put in the SkyBlock lobby!" -> {
                if (sendMsg) sendPartyMessage("$CHAT_PREFIX You were kicked while joining that server!")
                lastKickTime = System.currentTimeMillis()
                showTime = true
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! timer) return
        if (! showTime) return
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
        SoundUtils.notificationSound()
        setTimeout(500) { SoundUtils.notificationSound() }
        setTimeout(1000) { SoundUtils.notificationSound() }
    }
}
