package com.github.noamm9.features.impl.misc

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.location.LocationUtils.inSkyblock
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import gg.essential.universal.UResolution

object SBKick: Feature("Shows a timer on screen for when you can rejoin Skyblock.") {
    private val sendMsg by ToggleSetting("Send Party Message")

    private var showTime = false
    private var lastKickTime = System.currentTimeMillis()

    override fun init() {
        register<ChatMessageEvent> {
            when (event.component.unformattedText) {
                "There was a problem joining SkyBlock, try again in a moment!" -> {
                    if (showTime) return@register
                    lastKickTime = System.currentTimeMillis()
                    showTime = true
                }

                "You were kicked while joining that server!" -> {
                    if (showTime) return@register
                    if (sendMsg.value) ChatUtils.sendPartyMessage("You were kicked while joining that server!")
                    lastKickTime = System.currentTimeMillis()
                    showTime = true
                }

                "A kick occurred in your connection, so you were put in the SkyBlock lobby!" -> {
                    if (showTime) return@register
                    if (sendMsg.value) ChatUtils.sendPartyMessage("You were kicked while joining that server!")
                    lastKickTime = System.currentTimeMillis()
                    showTime = true
                }
            }
        }

        register<RenderOverlayEvent> {
            if (! showTime) return@register
            val timeSinceKick = System.currentTimeMillis() - lastKickTime
            if (inSkyblock && timeSinceKick > 10_000) {
                showTime = false
                return@register
            }

            if (timeSinceKick >= 60_000) showTime = false
            else event.context.drawCenteredString(
                "§cLast kicked from SkyBlock §b${(timeSinceKick / 1000.0).toFixed(2)}s ago",
                UResolution.scaledWidth / 2f,
                UResolution.scaledHeight / 2f - 20,
                scale = 1.5f
            )
        }
    }
}