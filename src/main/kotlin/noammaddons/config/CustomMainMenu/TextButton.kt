package noammaddons.config.CustomMainMenu

import net.minecraft.client.renderer.GlStateManager
import noammaddons.utils.MouseUtils.isElementHovered
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import java.awt.Color

class TextButton(val text: String, var x: Int, var y: Int) {
	val w: Int = 150
	val h: Int = 20
	
	
	fun renderButton(x: Int, y: Int, mouseX: Int, mouseY: Int) {
		this.x = x
		this.y = y
		
		if (isHovered(mouseX, mouseY)) {
			drawRoundedBorder(
				Color(255, 255, 255),
				x.toDouble(),
				y.toDouble(),
				w.toDouble(),
				h.toDouble(),
				5.0, 1.0, 2
			)
		}
		
		drawRoundedRect(
			Color(33, 33, 33, 255),
			x.toDouble(),
			y.toDouble(),
			w.toDouble(),
			h.toDouble()
		)
		
		drawCenteredText(
			text,
			(x + w / 2f).toDouble(),
			(y + h / 2f - 4).toDouble(),
			1.0, Color(0, 255, 255)
		)
		
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1f)
	}
	
	fun isHovered(mouseX: Int, mouseY: Int): Boolean {
		return isElementHovered(
			mouseX.toFloat(),
			mouseY.toFloat(),
			x.toDouble(),
			y.toDouble(),
			w,
			h
		)
	}
}