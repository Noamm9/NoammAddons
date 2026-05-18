package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import java.awt.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ClockDisplay: Feature("Displays the system time on screen.") {
    private val seconds by ToggleSetting("Show Seconds", true)
    private val color by ColorSetting("Color", Color(255, 134, 0), false)

    override fun init() {
        hudElement("ClockDisplay") { ctx, _ ->
            val text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm${if (seconds.value) ":ss" else ""}"))
            Render2D.drawString(ctx, text, 0, 0, color.value)
            return@hudElement text.width().toFloat() to 9f
        }
    }
}