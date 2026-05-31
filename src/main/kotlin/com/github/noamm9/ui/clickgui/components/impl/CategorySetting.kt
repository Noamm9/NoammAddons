package com.github.noamm9.ui.clickgui.components.impl

import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class CategorySetting(name: String): Setting<Unit>(name, Unit) {
    override val height = 22

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        Render2D.drawRect(ctx, x, y, width, height, Color(255, 255, 255, 10))
        Render2D.drawCenteredString(ctx, "§l$name", x + width / 2, y + 7, Style.accentColor, 1, true)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) = false
}