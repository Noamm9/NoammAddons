package noammaddons.config.EditGui

import noammaddons.noammaddons.Companion.hudData
import noammaddons.utils.ChatUtils
import noammaddons.utils.MouseUtils
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import net.minecraft.client.gui.GuiScreen
import noammaddons.sounds.click
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.sign


class HudEditorScreen: GuiScreen() {
    private var elements = ElementsManager.elements
    private var selectedElement: HudElement? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private val resetButton = ChatUtils.Text("Reset All Elements", 0f, 0f, 1f)
    private var resetButtonWidth = 0f


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        elements.forEach { it.draw(true) }
        resetButtonWidth = mc.fontRendererObj.getStringWidth(resetButton.text) * resetButton.scale

        resetButton.x = mc.getWidth()/2 - resetButtonWidth/2
        resetButton.y = mc.getHeight()*0.75f


        RenderUtils.drawRoundedRect(
            Color(33,33,33).darker(),
            resetButton.x - resetButtonWidth/7.5f,
            resetButton.y - resetButtonWidth/7.5f,
            resetButtonWidth + resetButtonWidth/3.75f,
            8f * resetButton.scale + resetButtonWidth/3.75f
        )

        RenderUtils.drawRoundedRect(
            Color(33,33,33),
            resetButton.x - resetButtonWidth/15f,
            resetButton.y - resetButtonWidth/15f,
            resetButtonWidth + resetButtonWidth/7.5f,
            8f * resetButton.scale + resetButtonWidth/7.5f
        )

        RenderUtils.drawText(
            resetButton.text,
            resetButton.x,
            resetButton.y,
            resetButton.scale,
            Color.CYAN
        )
    }



    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 0) { // Left-click
            elements.forEach {
                if (it.isHovered(mouseX.toFloat(), mouseY.toFloat())) {
                    selectedElement = it
                    offsetX = mouseX - it.getX()
                    offsetY = mouseY - it.getY()
                }
            }

            if (MouseUtils.isElementHovered(
                    mouseX.toFloat(), mouseY.toFloat(),
                    resetButton.x - resetButtonWidth/7.5,
                    resetButton.y - resetButtonWidth/7.5,
                    (resetButtonWidth + resetButtonWidth/3.75).toInt(),
                    (8.0 * resetButton.scale + resetButtonWidth/3.75).toInt()
                )) {
                selectedElement = null
	            click.play()
                elements.forEach { it.reset() }
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        selectedElement = null
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        selectedElement?.let {
            it.setX(mouseX - offsetX)
            it.setY(mouseY - offsetY)
        }
    }

    override fun handleMouseInput() {
        val scrollEvent = Mouse.getEventDWheel()
        if (scrollEvent != 0) {
            selectedElement?.let {
                it.setScale(
	                it.getScale() + scrollEvent.sign * 0.05f
				)
            }
        }
        super.handleMouseInput()
    }

    override fun onGuiClosed() {
        hudData.save()
        super.onGuiClosed()
    }

    override fun doesGuiPauseGame(): Boolean {
        return false
    }
}
