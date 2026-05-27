package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.height
import com.github.noamm9.utils.render.Render2D.width
import java.awt.Color

object FpsDisplay: Feature("Displays the game's FPS on screen.") {
    private val color by ColorSetting("Color", Color(230, 114, 230), false)

    override fun init() {
        hudElement("FpsDisplay") { ctx, _ ->
            val text = "${mc.fps} fps"
            Render2D.drawString(ctx, text, 0, 0, color.value)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }
    }
}