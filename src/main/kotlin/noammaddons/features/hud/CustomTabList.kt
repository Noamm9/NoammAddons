package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.renderPlayerlist
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.MathUtils.calculateScaleFactor
import noammaddons.utils.Utils.splitArray
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import noammaddons.utils.TablistUtils.getTabList
import noammaddons.utils.TablistUtils.getTabListFooterText
import java.awt.Color

object CustomTabList {
	@SubscribeEvent
	fun drawCustomTabList(event: renderPlayerlist) {
		if (!config.CustomTabList) return
		event.isCanceled = true
		
		val names = getTabList.map { it.second }
		val footerLines = (
			getTabListFooterText()?.split("\n")
			?.toMutableList() ?: mutableListOf()
		).toMutableList().filterNot { it.removeFormatting().contains("hypixel.net", true) }
		
		var maxNameWidth = 0
		names.forEach { name ->
			maxNameWidth = maxNameWidth.coerceAtLeast(mc.fontRendererObj.getStringWidth(name.removeFormatting()))
		}
		
		var maxFooterWidth = 0
		footerLines.forEach { line ->
			maxFooterWidth = maxOf(maxFooterWidth, mc.fontRendererObj.getStringWidth(line.removeFormatting()))
		}
		
		val screenWidth = mc.getWidth()
		val screenHeight = mc.getHeight()
		val fontHeight = 9
		val scaleFactor = calculateScaleFactor(screenWidth, screenHeight)
		val rowsCount = splitArray(names, 20).size.coerceIn(1, 4)
		
		val tableWidth = (maxNameWidth + 20) * rowsCount
		val tableHeight = fontHeight * 25
		
		val xOffset = (screenWidth - tableWidth) / 2
		val yOffset = screenHeight / 20
		
		drawRoundedRect(
			Color(33, 33, 33, 153).darker(),
			xOffset - tableWidth / 50,
			yOffset - tableWidth / 50,
			tableWidth + tableWidth / 25,
			tableHeight + tableWidth / 25
		)
		drawRoundedRect(
			Color(33, 33, 33, 153),
			xOffset,
			yOffset,
			tableWidth,
			tableHeight
		)
		
		// Draw the player names
		splitArray(names, 20).forEachIndexed { index, row ->
			if (index >= 4) return@forEachIndexed
			
			row.forEachIndexed { i, line ->
				drawText(
					line,
					xOffset + ((maxNameWidth + 20f) * index) + 10,
					yOffset + fontHeight * ((i+1) *1.15f),
					scaleFactor
				)
			}
		}
		
		// Draw the footer if it exists
		if (footerLines.isNotEmpty()) {
			val footerWidth = maxFooterWidth + 20
			val footerHeight = fontHeight * footerLines.size + 10
			
			val footerXOffset = screenWidth / 2 - footerWidth / 2
			val footerYOffset = yOffset + tableHeight + 30
			
			
			drawRoundedRect(
				Color(33, 33, 33, 153).darker(),
				footerXOffset - maxFooterWidth / 50, footerYOffset - maxFooterWidth / 50,
				footerWidth + footerWidth / 25,
				footerHeight + footerWidth / 25
			)
			drawRoundedRect(
				Color(33, 33, 33, 153),
				footerXOffset,
				footerYOffset,
				footerWidth,
				footerHeight
			)
			
			footerLines.forEachIndexed { i, line ->
				drawText(
					line,
					footerXOffset + (footerWidth - mc.fontRendererObj.getStringWidth(line)) / 2f,
					footerYOffset + fontHeight * (i + 1f)-5,
					scaleFactor
				)
			}
		}
	}
}
