package noammaddons.config.EditGui

import gg.essential.universal.UGraphics.getStringWidth
import net.minecraft.client.gui.GuiScreen
import noammaddons.config.EditGui.ElementsManager.DrawableElement
import noammaddons.config.EditGui.ElementsManager.posElements
import noammaddons.config.EditGui.ElementsManager.textElements
import noammaddons.noammaddons.Companion.hudData
import noammaddons.utils.MouseUtils.isElementHovered
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.SoundUtils.click
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.sign


class HudEditorScreen: GuiScreen() {
    private val elements: List<DrawableElement> = textElements + posElements
    private var selectedElement: DrawableElement? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var resetButtonX = 0f
    private var resetButtonY = 0f
    private var resetButtonS = 1f
    private var resetButtonWidth = 0f


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        elements.forEach { it.draw(true) }
        resetButtonWidth = getStringWidth("Reset All Elements") * resetButtonS

        resetButtonX = mc.getWidth() / 2 - resetButtonWidth / 2
        resetButtonY = mc.getHeight() * 0.75f


        drawRoundedRect(
            if (
                isElementHovered(
                    mouseX.toFloat(), mouseY.toFloat(),
                    resetButtonX - resetButtonWidth / 7.5,
                    resetButtonY - resetButtonWidth / 7.5,
                    (resetButtonWidth + resetButtonWidth / 3.75).toInt(),
                    (8 * resetButtonS + resetButtonWidth / 3.75).toInt()
                )
            ) Color.WHITE.darker()
            else Color(33, 33, 33).darker(),
            resetButtonX - resetButtonWidth / 7.5f,
            resetButtonY - resetButtonWidth / 7.5f,
            resetButtonWidth + resetButtonWidth / 3.75f,
            8f * resetButtonS + resetButtonWidth / 3.75f
        )

        drawRoundedRect(
            Color(33, 33, 33),
            resetButtonX - resetButtonWidth / 15f,
            resetButtonY - resetButtonWidth / 15f,
            resetButtonWidth + resetButtonWidth / 7.5f,
            9 + resetButtonWidth / 7.5f
        )

        RenderUtils.drawText(
            "Reset All Elements",
            resetButtonX,
            resetButtonY,
            resetButtonS,
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
                    click.start()
                }
            }

            if (isElementHovered(
                    mouseX.toFloat(), mouseY.toFloat(),
                    resetButtonX - resetButtonWidth / 7.5,
                    resetButtonY - resetButtonWidth / 7.5,
                    (resetButtonWidth + resetButtonWidth / 3.75).toInt(),
                    (8 * resetButtonS + resetButtonWidth / 3.75).toInt()
                )
            ) {
                selectedElement = null
                click.start()
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
        return true
    }
}
