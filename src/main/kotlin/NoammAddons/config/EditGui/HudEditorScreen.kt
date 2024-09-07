package NoammAddons.config.EditGui

import NoammAddons.NoammAddons.Companion.hudData
import NoammAddons.utils.ChatUtils
import NoammAddons.utils.MouseUtils
import NoammAddons.utils.RenderUtils
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.sign


class HudEditorScreen(private val elements: MutableList<HudElement>) : GuiScreen() {
    private var selectedElement: HudElement? = null
    private var offsetX = 0.0
    private var offsetY = 0.0
    private val resetButton = ChatUtils.Text("Reset All Elements", .0, .0, 1.0)
    private var resetButtonWidth = .0


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        elements.forEach { it.draw(true) }
        resetButtonWidth = mc.fontRendererObj.getStringWidth(resetButton.text) * resetButton.scale

        resetButton.x = mc.getWidth()/2 - resetButtonWidth/2
        resetButton.y = mc.getHeight()*.75


        RenderUtils.drawRoundedRect(
            Color(33,33,33).darker(),
            resetButton.x - resetButtonWidth/7.5,
            resetButton.y - resetButtonWidth/7.5,
            resetButtonWidth + resetButtonWidth/3.75,
            8.0 * resetButton.scale + resetButtonWidth/3.75
        )

        RenderUtils.drawRoundedRect(
            Color(33,33,33),
            resetButton.x - resetButtonWidth/15,
            resetButton.y - resetButtonWidth/15,
            resetButtonWidth + resetButtonWidth/7.5,
            8.0 * resetButton.scale + resetButtonWidth/7.5
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
                it.setScale(it.getScale() + scrollEvent.sign * 0.05)
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
