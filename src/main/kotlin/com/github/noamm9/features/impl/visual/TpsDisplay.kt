package com.github.noamm9.features.impl.visual

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ServerUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.height
import com.github.noamm9.utils.render.Render2D.width
import java.awt.Color


object TpsDisplay: Feature("Displays the Server's Ticks Per Second (TPS) on screen.") {
    private val color by ColorSetting("Color", Color(0, 114, 255), false)

    override fun init() {
        hudElement("TpsDisplay") { ctx, example ->
            val text = "TPS: &f${if (example) 20 else ServerUtils.tps.toFixed(1)}"
            Render2D.drawString(ctx, text, 0, 0, color.value)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }
    }
}