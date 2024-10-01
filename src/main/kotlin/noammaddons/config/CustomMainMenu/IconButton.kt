package noammaddons.config.CustomMainMenu

import net.minecraft.util.ResourceLocation
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.utils.MouseUtils.isElementHovered
import noammaddons.utils.RenderUtils.drawRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.renderTexture
import java.awt.Color


class IconButton(var icon: String, var borderWidth: Double = 4.0, var x: Int, var y: Int, val w: Int = 30, val h: Int = 30) {
	val iconImage: ResourceLocation = ResourceLocation(MOD_ID, "menu/${icon}.png")
	
	
	
	fun renderButton(x: Int, y: Int, mouseX: Double, mouseY: Double) {
		val bx: Double = x.toDouble()
		val by: Double = y.toDouble()
		val bw = w.toDouble()
		val bh = h.toDouble()
		this.x = x
		this.y = y
		val color = if (isHovered(mouseX.toInt(), mouseY.toInt())) Color.WHITE
			else Color(33, 33, 33, 255).darker()
		
		
		drawRoundedRect(Color(33, 33, 33, 102), bx, by, bw, bh)
		drawRoundedBorder(color, bx, by, bw, bh, thickness = borderWidth, drawMode = 1)
		
		renderTexture(iconImage, x, y, w, h)
	}
	
	fun isHovered(mouseX: Int, mouseY: Int): Boolean {
		return isElementHovered(
			mouseX.toFloat(),
			mouseY.toFloat(),
			x.toDouble(),
			y.toDouble(),
			w, h
		)
	}
}