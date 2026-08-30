package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.dungeons.DungeonUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import kotlin.math.roundToInt

object WarpCooldown: Feature("Displays on screen how long until you can start another dungeon run.") {
    private var startTime = System.currentTimeMillis()
    private var onCd = false

    override fun init() {
        hudElement("WarpCooldown", shouldDraw = ::onCd::get) { ctx, example ->
            val remaining = (30 - (System.currentTimeMillis() - startTime) / 1000.0).roundToInt()
            if (remaining < 0) onCd = false
            val text = "&bWarp Cooldown: &f${if (example) 30 else remaining}s"
            ctx.drawString(text, 0, 0)
            return@hudElement text.width() to 9
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.onHypixel) return@register
            if (onCd) return@register
            if (! event.unformattedText.matches(DungeonUtils.floorEnterRegex)) return@register
            startTime = System.currentTimeMillis()
            onCd = true
        }
    }
}