package noammaddons.config.EditGui

import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import noammaddons.config.EditGui.ElementsManager.DrawableElement
import noammaddons.config.EditGui.ElementsManager.posElements
import noammaddons.config.EditGui.ElementsManager.textElements
import noammaddons.config.EditGui.components.PosElement
import noammaddons.noammaddons.Companion.hudData
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.SoundUtils.click
import org.lwjgl.input.Mouse
import kotlin.math.sign


class HudEditorScreen: GuiScreen() {
    private val elements: List<DrawableElement> = textElements + posElements
    private var selectedElement: DrawableElement? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private val resetButtonStr = "&b  Reset All Elements  ".addColor()

    private val scale: Float get() = 2f / mc.getScaleFactor()

    override fun initGui() {
        val width = getStringWidth(resetButtonStr).toInt()

        buttonList.add(
            GuiButton(
                69,
                ((mc.getWidth() / (2 * scale)) - width / 2).toInt(),
                (mc.getHeight() / (scale * 1.33f)).toInt(),
                width, 20,
                resetButtonStr
            )
        )

        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)
        textElements.forEach { it.draw(true) }
        GlStateManager.popMatrix()
        posElements.forEach { it.draw(true) }

        buttonList[0].drawButton(mc, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale

        if (mouseButton == 0) { // Left-click
            textElements.forEach { element ->
                if (element.isHovered(scaledMouseX, scaledMouseY)) {
                    selectedElement = element
                    offsetX = scaledMouseX - element.getX()
                    offsetY = scaledMouseY - element.getY()
                    click.start()
                }
            }

            posElements.forEach { element ->
                if (element.isHovered(mouseX.toFloat(), mouseY.toFloat())) {
                    selectedElement = element
                    offsetX = mouseX - element.getX()
                    offsetY = mouseY - element.getY()
                    click.start()
                }
            }

            if (buttonList[0].mousePressed(mc, mouseX, mouseY)) {
                selectedElement = null
                click.start()
                elements.forEach { it.reset() }
            }
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale

        selectedElement?.let {
            if (it is PosElement) {
                it.setX(mouseX - offsetX)
                it.setY(mouseY - offsetY)
            }
            else {
                it.setX(scaledMouseX - offsetX)
                it.setY(scaledMouseY - offsetY)
            }
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

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        selectedElement = null
    }

    override fun doesGuiPauseGame(): Boolean {
        return true
    }
}

