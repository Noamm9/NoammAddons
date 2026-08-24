package com.github.noamm9.ui.clickgui.components.settings.impl

import com.github.noamm9.config.types.UnitSetting
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.components.settings.Widget
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class CategoryWidget(name: String): Widget<Unit>(UnitSetting(name)) {
    override val height = 22

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        ctx.drawRect(x, y, width, height, Color(255, 255, 255, 10))
        ctx.drawCenteredString("§l$name", x + width / 2, y + 7, Style.accentColor)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) = false
}