package noammaddons.config.EditGui

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import noammaddons.NoammAddons
import noammaddons.NoammAddons.Companion.hudData
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.utils.MouseUtils.isMouseOver
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawRectBorder
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color

// Inspired by https://github.com/kiwidotzip/zen/blob/master/src/main/kotlin/meowing/zen/hud/HUDEditor.kt
object HudEditorScreen: GuiScreen() {
    val elements = mutableListOf<GuiElement>()

    private var dragging: GuiElement? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var selected: GuiElement? = null
    private var gridSize = 10
    private var showResetConfirm = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val actualMouseX = Mouse.getX() * width / mc.displayWidth
        val actualMouseY = height - Mouse.getY() * height / mc.displayHeight - 1
        val sr = ScaledResolution(mc)

        drawBackground(sr)

        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()

        drawGrid(sr)
        elements.filter { it.enabled }.forEach {
            it.renderBackground(it.isHovered(actualMouseX.toFloat(), actualMouseY.toFloat()))
            it.exampleDraw()
        }
        drawTooltips(sr)

        drawResetButton(sr, actualMouseX, actualMouseY)
        if (showResetConfirm) {
            drawResetConfirmation(actualMouseX, actualMouseY, sr)
        }

        GlStateManager.popMatrix()
        super.drawScreen(mouseX, mouseY, partialTicks)

        dragging?.let { element ->
            element.setX((actualMouseX - dragOffsetX))
            element.setY((actualMouseY - dragOffsetY))
        }
    }

    private fun drawBackground(sr: ScaledResolution) {
        drawGradientRect(0, 0, sr.scaledWidth, sr.scaledHeight, Color(15, 15, 25, 200).rgb, Color(25, 25, 35, 200).rgb)
    }

    private fun drawGrid(sr: ScaledResolution) {
        val color = Color(60, 60, 80, 100)
        val tess = Tessellator.getInstance()
        val worldRenderer = tess.worldRenderer

        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR)

        for (x in 0 until sr.scaledWidth step gridSize) {
            worldRenderer.pos(x.toDouble(), 0.0, 0.0).color(color.red, color.green, color.blue, color.alpha).endVertex()
            worldRenderer.pos(x.toDouble(), sr.scaledHeight.toDouble(), 0.0).color(color.red, color.green, color.blue, color.alpha).endVertex()
        }

        for (y in 0 until sr.scaledHeight step gridSize) {
            worldRenderer.pos(0.0, y.toDouble(), 0.0).color(color.red, color.green, color.blue, color.alpha).endVertex()
            worldRenderer.pos(sr.scaledWidth.toDouble(), y.toDouble(), 0.0).color(color.red, color.green, color.blue, color.alpha).endVertex()
        }

        tess.draw()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    private fun drawTooltips(sr: ScaledResolution) {
        val tooltip = when {
            selected != null -> "Editing: ${selected !!::class.simpleName}"
            else -> null
        }

        tooltip?.let { text ->
            val x = ((sr.scaledWidth - textRenderer.getStringWidth(text)) / 2).toInt()
            val y = sr.scaledHeight - 30

            val a = x - 5
            val b = y - 3
            val c = textRenderer.getStringWidth(text) + 10
            val d = 13

            RenderUtils.drawRect(Color(30, 35, 45, 140), a, b, c, d)
            drawRectBorder(Color(100, 180, 255), a, b, c, d)
            textRenderer.drawText(text, x, y, Color(235, 235, 235))
        }
    }

    private fun drawResetButton(sr: ScaledResolution, mx: Int, my: Int) {
        val str = "Reset HUD"
        val width = textRenderer.getStringWidth(str)
        val height = 9
        val x = (sr.scaledWidth - width) / 2
        val y = ((sr.scaledHeight + height) * 0.85).toInt()

        val isHovered = isMouseOver(mx, my, x - 4, y - 4, width + 8, height + 8)
        val alpha = if (isHovered) 140 else 90
        val borderColor = when {
            isHovered -> Color(100, 180, 255)
            else -> Color(100, 100, 120)
        }

        RenderUtils.drawRect(Color(30, 35, 45, alpha), x - 4, y - 4, width + 8, height + 8)
        drawRectBorder(borderColor, x - 4, y - 4, width + 8, height + 8)
        textRenderer.drawText(str, x - 1, y)
    }

    private fun drawResetConfirmation(mouseX: Int, mouseY: Int, sr: ScaledResolution) {
        val popupWidth = 280
        val popupHeight = 120
        val popupX = (sr.scaledWidth - popupWidth) / 2
        val popupY = (sr.scaledHeight - popupHeight) / 2

        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, 0f, 300f)

        drawRect(0, 0, sr.scaledWidth, sr.scaledHeight, Color(0, 0, 0, 120).rgb)
        drawRect(popupX, popupY, popupX + popupWidth, popupY + popupHeight, Color(25, 25, 35, 240).rgb)
        drawRectBorder(Color(70, 130, 180, 255), popupX, popupY, popupWidth, popupHeight)

        val titleText = "Reset All Elements"
        val titleX = popupX + (popupWidth - textRenderer.getStringWidth(titleText)) / 2
        textRenderer.drawText(titleText, titleX, popupY + 15f, Color(220, 100, 100))

        val message = listOf("This will reset all HUD elements", "to their default positions")
        val messageX = popupX + (popupWidth - textRenderer.getStringWidth(message)) / 2
        textRenderer.drawText(message, messageX, popupY + 55, Color(200, 200, 200))

        val buttonWidth = 80
        val buttonHeight = 20
        val buttonSpacing = 20
        val confirmX = popupX + (popupWidth / 2) - buttonWidth - (buttonSpacing / 2)
        val cancelX = popupX + (popupWidth / 2) + (buttonSpacing / 2)
        val buttonY = popupY + popupHeight - 35

        val confirmHovered = mouseX in confirmX .. (confirmX + buttonWidth) && mouseY in buttonY .. (buttonY + buttonHeight)
        val cancelHovered = mouseX in cancelX .. (cancelX + buttonWidth) && mouseY in buttonY .. (buttonY + buttonHeight)

        val confirmBg = if (confirmHovered) Color(200, 80, 80, 200).rgb else Color(170, 60, 60, 180).rgb
        val cancelBg = if (cancelHovered) Color(60, 120, 180, 200).rgb else Color(40, 100, 160, 180).rgb

        drawRect(confirmX, buttonY, confirmX + buttonWidth, buttonY + buttonHeight, confirmBg)
        drawRect(cancelX, buttonY, cancelX + buttonWidth, buttonY + buttonHeight, cancelBg)

        drawRectBorder(Color(255, 120, 120, 255), confirmX, buttonY, buttonWidth, buttonHeight)
        drawRectBorder(Color(120, 180, 255, 255), cancelX, buttonY, buttonWidth, buttonHeight)

        val confirmText = "Reset"
        val cancelText = "Cancel"
        val confirmTextX = confirmX + (buttonWidth - textRenderer.getStringWidth(confirmText)) / 2 - 1
        val cancelTextX = cancelX + (buttonWidth - textRenderer.getStringWidth(cancelText)) / 2 - 1
        val textY = buttonY + 6

        textRenderer.drawText(confirmText, confirmTextX, textY)
        textRenderer.drawText(cancelText, cancelTextX, textY)

        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val actualMouseX = Mouse.getX() * width / mc.displayWidth
        val actualMouseY = height - Mouse.getY() * height / mc.displayHeight - 1

        if (showResetConfirm) return handleResetConfirmClick(actualMouseX, actualMouseY)
        handleResetButtonClick(actualMouseX, actualMouseY, mouseButton)

        if (mouseButton == 0) handleElementDrag(actualMouseX, actualMouseY)
        else if (mouseButton == 1) elements.find { it.isHovered(actualMouseX.toFloat(), actualMouseY.toFloat()) }?.let {
            selected = it
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun handleResetButtonClick(mx: Int, my: Int, btn: Int) {
        if (btn != 0) return

        val str = "Reset HUD"
        val sr = ScaledResolution(mc)
        val width = textRenderer.getStringWidth(str)
        val height = 9
        val x = (sr.scaledWidth - width) / 2
        val y = ((sr.scaledHeight + height) * 0.85).toInt()

        if (! isMouseOver(mx, my, x - 4, y - 4, width + 8, height + 8)) return

        showResetConfirm = true
    }

    private fun handleResetConfirmClick(mouseX: Int, mouseY: Int) {
        val sr = ScaledResolution(mc)
        val popupWidth = 280
        val popupHeight = 120
        val popupX = (sr.scaledWidth - popupWidth) / 2
        val popupY = (sr.scaledHeight - popupHeight) / 2

        val buttonWidth = 80
        val buttonHeight = 20
        val buttonSpacing = 20
        val confirmX = popupX + (popupWidth / 2) - buttonWidth - (buttonSpacing / 2)
        val cancelX = popupX + (popupWidth / 2) + (buttonSpacing / 2)
        val buttonY = popupY + popupHeight - 35

        when {
            mouseX in confirmX .. (confirmX + buttonWidth) && mouseY in buttonY .. (buttonY + buttonHeight) -> {
                resetAll()
                showResetConfirm = false
            }

            mouseX in cancelX .. (cancelX + buttonWidth) && mouseY in buttonY .. (buttonY + buttonHeight) -> {
                showResetConfirm = false
            }

            mouseX !in popupX .. (popupX + popupWidth) || mouseY !in popupY .. (popupY + popupHeight) -> {
                showResetConfirm = false
            }
        }
    }

    private fun handleElementDrag(mouseX: Int, mouseY: Int) {
        elements.find { it.isHovered(mouseX.toFloat(), mouseY.toFloat()) }?.let { element ->
            dragging = element
            selected = element
            dragOffsetX = mouseX - element.getX()
            dragOffsetY = mouseY - element.getY()
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging?.let { dragging = null }
        selected = null
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        val dWheel = Mouse.getEventDWheel()
        if (dWheel != 0 && selected != null) {
            val scaleDelta = if (dWheel > 0) 0.1f else - 0.1f
            selected !!.setScale((selected !!.getScale() + scaleDelta).coerceIn(0.2f, 5f))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (showResetConfirm) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_N) {
                showResetConfirm = false
            }
            else if (keyCode == Keyboard.KEY_Y || keyCode == Keyboard.KEY_RETURN) {
                resetAll()
                showResetConfirm = false
            }
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    private fun resetAll() = elements.forEach { it.reset() }

    override fun onGuiClosed() {
        super.onGuiClosed()
        hudData.save()
    }

    override fun doesGuiPauseGame(): Boolean {
        return true
    }

    fun isOpen(): Boolean {
        val gui = NoammAddons.mc.currentScreen ?: return false
        return gui is HudEditorScreen
    }
}