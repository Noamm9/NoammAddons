package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.ColorConfig
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.height
import com.github.noamm9.utils.render.RenderHelper.width
import java.awt.Color

object FreezeDisplay: Feature("Shows how long the server froze after a chosen threshold.") {
    private val color by ColorConfig("Color", Color(245, 73, 39), false)
    private val threshold by NumberConfig("Threshold", 500, 100, 2000, 100)
    private val dungeonsOnly by BooleanConfig("Only in Dungeons", true)

    private var lastPacketTime = System.currentTimeMillis()

    override fun init() {
        hudElement(this::class.simpleName !!, shouldDraw = shouldDraw, centered = true) { ctx, example ->
            val diff = System.currentTimeMillis() - lastPacketTime
            val text = if (example) "567ms" else "${diff}ms"

            ctx.drawCenteredString(text, 0, 0, color.value)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }

        register<WorldChangeEvent> { lastPacketTime = System.currentTimeMillis() }
        register<TickEvent.Server> { lastPacketTime = System.currentTimeMillis() }
    }

    private val shouldDraw = fun(): Boolean {
        if (dungeonsOnly.value && ! LocationUtils.inDungeon) return false
        if (mc.isLocalServer) return false

        return (System.currentTimeMillis() - lastPacketTime) > threshold.value
    }
}