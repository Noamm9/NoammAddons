package noammaddons.config.EditGui

import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.client.config.GuiUtils
import noammaddons.features.impl.DevOptions
import noammaddons.NoammAddons
import noammaddons.NoammAddons.Companion.hudData
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.sign


object HudEditorScreen: GuiScreen() {
    val elements = mutableListOf<GuiElement>()
    fun isOpen(): Boolean {
        val gui = NoammAddons.mc.currentScreen ?: return false
        return gui is HudEditorScreen
    }

    private var selectedElement: GuiElement? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private val resetButtonStr = "&b  Reset All Elements  ".addColor()

    private val scale: Float get() = 1f

    override fun initGui() {
        super.initGui()
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

    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GlStateManager.pushMatrix()

        elements.forEach {
            if (! it.enabled) return@forEach
            if (DevOptions.devMode) RenderUtils.drawRect(
                Color.WHITE, it.getX(), it.getY(), it.width * it.getScale(), (it.height * it.getScale()).toInt()
            )
            it.exampleDraw()
        }

        if (selectedElement != null) {
            val text = selectedElement?.javaClass?.simpleName ?: "Unknown"

            GlStateManager.translate(
                (mouseX - getStringWidth(text) / 2f) / scale,
                mouseY / scale,
                0f
            )

            GuiUtils.drawHoveringText(
                listOf(selectedElement?.javaClass?.simpleName ?: "Unknown"),
                0, 0, 232323, 232323,
                - 1, mc.fontRendererObj
            )
        }

        GlStateManager.popMatrix()


        if (DevOptions.devMode) drawText(
            "X: $mouseX, Y: $mouseY",
            20f, 20f, 3,
        )

        buttonList[0].drawButton(mc, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val scaledMouseX = mouseX / scale
        val scaledMouseY = mouseY / scale
        debugMessage("mouseX: $scaledMouseX, mouseY: $scaledMouseY")

        if (mouseButton == 0) { // Left-click
            elements.forEach { element ->
                if (! element.enabled) return@forEach
                if (! element.isHovered(scaledMouseX, scaledMouseY)) return@forEach
                selectedElement = element
                offsetX = scaledMouseX - element.getX()
                offsetY = scaledMouseY - element.getY()
                SoundUtils.click()

            }

            if (buttonList[0].mousePressed(mc, mouseX, mouseY)) {
                selectedElement = null
                SoundUtils.click()
                elements.forEach { it.reset() }
            }
        }

        if (! DevOptions.devMode) return
        if (mouseButton != 1) return // Right-click


        elements.forEach { element ->
            if (! element.enabled) return@forEach
            if (! element.isHovered(scaledMouseX, scaledMouseY)) return@forEach
            modMessage("Hoverd on ${element::class.simpleName}")
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        selectedElement?.let {
            it.setX(mouseX / scale - offsetX)
            it.setY(mouseY / scale - offsetY)
        }
    }

    override fun handleMouseInput() {
        val scrollEvent = Mouse.getEventDWheel()
        if (scrollEvent != 0) {
            selectedElement?.let {
                it.setScale(
                    (it.getScale() + scrollEvent.sign * 0.05f).coerceAtLeast(0.1f)
                )
            }
        }
        super.handleMouseInput()
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        hudData.save()
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        selectedElement = null
    }

    override fun doesGuiPauseGame(): Boolean {
        return true
    }
}

