package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderBoundingBox
import com.github.noamm9.utils.render.RenderHelper.renderVec
import java.awt.Color

@AlwaysActive
object PlayerESP: Feature("Attaches tracers to specific players. Use /na esp <player> to toggle a target.") {
    private val tracer by ToggleSetting("Tracer", true).withDescription("Draws a line from your crosshair to the targeted player.")
    private val box by ToggleSetting("Box", true).withDescription("Draws a box around the targeted player.")
    private val showName by ToggleSetting("Show Name", false).withDescription("Renders the targeted player's name above them.")
    private val espColor by ColorSetting("Color", Color.RED, false).withDescription("Color of the tracer, box and name.")

    private val targets = mutableSetOf<String>()

    override fun init() {
        register<WorldChangeEvent> { targets.clear() }

        register<RenderWorldEvent> {
            if (targets.isEmpty()) return@register
            val self = mc.player ?: return@register

            for (player in mc.level?.players() ?: return@register) {
                if (player === self) continue
                val name = player.gameProfile.name ?: continue
                if (name.lowercase() !in targets) continue

                val color = espColor.value

                if (tracer.value) Render3D.renderTracer(event.ctx, player.renderVec.add(y = player.bbHeight / 2.0), color)
                if (box.value) Render3D.renderBoxBounds(event.ctx, player.renderBoundingBox.inflate(0.05), color, outline = true, fill = false, phase = true)
                if (showName.value) Render3D.renderString(name, player.renderVec.add(y = player.bbHeight + 0.5), color, phase = true)
            }
        }
    }

    /** Adds the name if it isn't targeted, otherwise removes it. Returns true if now tracing. */
    fun toggleTarget(name: String): Boolean {
        val key = name.lowercase()
        return if (targets.add(key)) true else { targets.remove(key); false }
    }

    fun clearTargets() = targets.clear()

    val targetCount: Int get() = targets.size
}
