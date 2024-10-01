package noammaddons.config.CustomMainMenu

import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.util.ResourceLocation
import noammaddons.config.Config
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.sounds.click
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.renderTexture


class TitleScreen: GuiScreen() {
	private val textButtons = ArrayList<TextButton>()
	private val iconButtons = ArrayList<IconButton>()
	
	init {
		textButtons.add(TextButton("Singleplayer", width / 2 - 75, height / 2))
		textButtons.add(TextButton("Multiplayer", width / 2 - 75, height / 2 + 25))
		textButtons.add(TextButton("Settings", width / 2 - 75, height / 2 + 50))
		iconButtons.add(IconButton("X", 4.0, width - 35, 8))
		iconButtons.add(IconButton("logo", 16.0, width / 2 - 50, height / 2 - 198, 100, 100))
	}
	

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
		super.drawScreen(mouseX, mouseY, partialTicks)
		renderTexture(
			ResourceLocation(MOD_ID, "menu/background.png"),
			0, 0, width, height,
		)
		
		for ((y, textButton) in textButtons.withIndex()) {
			textButton.renderButton(
				width / 2 - 75,
				height / 2 + y * 30,
				mouseX, mouseY
			)
		}
		
		for (iconButton in iconButtons) {
			var x = 0
			var y = 0
			when (iconButton.icon) {
				"X" -> {
					x = width - 38
					y = 5
				}
				"logo" -> {
                    x = width / 2 - 50
                    y = height / 2 - 198
                }
			}
			iconButton.renderButton(x, y, mouseX.toDouble(), mouseY.toDouble())
		}
		
		
		drawCenteredText(FULL_PREFIX, width/2.0, height/2.0 - 80, 3.0)
	}
	
	
	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
		for (textButton in textButtons) {
			if (textButton.isHovered(mouseX, mouseY)) {
				click.play()
				when (textButton.text) {
					"Singleplayer" -> mc.displayGuiScreen(GuiSelectWorld(this))
					"Multiplayer" -> mc.displayGuiScreen(GuiMultiplayer(this))
					"Settings" -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
				}
			}
		}
		
		for (iconButton in iconButtons) {
			if (iconButton.isHovered(mouseX, mouseY)) {
				click.play()
				when (iconButton.icon) {
					"X" -> mc.shutdown()
					"logo" -> openScreen(Config.gui())
				}
			}
		}
		
		super.mouseClicked(mouseX, mouseY, mouseButton)
	}
}