package com.github.noamm9.features.impl.tweaks

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.RenderOverlayEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.location.LocationUtils.inSkyblock
import com.github.noamm9.utils.render.Render2D

object SBKick: Feature("Shows a timer on screen for when u can join skyblock again.") {
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
            else Render2D.drawCenteredString(
                event.context,
                "§cLast kicked from SkyBlock §b${(timeSinceKick / 1000.0).toFixed(2)}s ago",
                mc.window.guiScaledWidth / 2f,
                mc.window.guiScaledHeight / 2f - 20,
                scale = 1.5f
            )
        }
    }
}