package noammaddons.features.gui

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.Slot
import noammaddons.features.Feature
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderUtils.drawFloatingRect
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color


/**
 * Slightly modified code from DulkirMod
 * https://github.com/Noamm9/DulkirMod/blob/master/src/main/kotlin/dulkirmod/features/ScalableTooltips.kt
 *
 *  @see noammaddons.mixins.MixinGuiUtils
 */
object ScalableTooltips: Feature() {
    private var scrollY: Int = 0
    private var scrollX: Int = 0
    private var snapFlag: Boolean = true
    private var scaleScale: Float = 0f
    private var lastShit: Pair<List<String>, Slot?>? = null

    @JvmStatic
    fun drawScaledHoveringText(
        textLines: List<String>,
        mouseX: Int,
        mouseY: Int,
        screenWidth: Int,
        screenHeight: Int,
        font: FontRenderer,
    ): Boolean {
        if (! config.ScalableTooltips) return false
        if (textLines.isEmpty()) return true
        val gui = mc.currentScreen
        val slot = (gui as? GuiContainer)?.slotUnderMouse
        if (lastShit == null) lastShit = Pair(textLines, slot)
        else if (lastShit?.second != slot || lastShit?.first != textLines) {
            lastShit = Pair(textLines, slot)
            resetPos()
        }


        val eventDWheel = Mouse.getDWheel()
        if (gui !is GuiChat && gui !is GuiContainerCreative) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                if (eventDWheel < 0) scrollX += mc.displayWidth / 192
                else if (eventDWheel > 0) scrollX -= mc.displayWidth / 192
            }
            else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                if (eventDWheel < 0) scaleScale -= .1f
                else if (eventDWheel > 0) scaleScale += .1f
            }
            else if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) resetPos()
            else {
                if (eventDWheel < 0) scrollY -= mc.displayHeight / 108
                else if (eventDWheel > 0) scrollY += mc.displayHeight / 108
            }

        }

        if (textLines.isNotEmpty() && ! textLines[0].contains("§f§o §r")) {
            val scale = (((3f * config.ScalableTooltipsScale) + scaleScale) / mc.getScaleFactor()).coerceAtLeast(1.5f / mc.getScaleFactor())

            var width = 0
            for (textLine in textLines) {
                val textWidth = font.getStringWidth(textLine)
                if (textWidth > width) width = textWidth
            }
            val height = (textLines.size) * font.FONT_HEIGHT


            GlStateManager.pushMatrix()
            GlStateManager.disableDepth()
            GlStateManager.disableLighting()
            GlStateManager.scale(scale, scale, scale)

            var x = ((mouseX + 12 + scrollX) / scale).toInt()
            var y = ((mouseY - 12 + scrollY) / scale).toInt()


            if ((x + width + 4 > screenWidth / scale) && (width + 4 <= screenWidth / scale)) {
                scrollX = (screenWidth - mouseX - 12 - (width + 4) * scale).toInt()
            }

            if ((y + height + 4 > screenHeight / scale) && (height + 4 <= screenHeight / scale)) {
                scrollY = (screenHeight - mouseY + 12 - (height + 4) * scale).toInt()
            }

            if (x < 0 && (width + 4 <= screenWidth / scale)) scrollX = - mouseX - 12 + 4
            if (y < 0 && (height + 4 <= screenHeight / scale)) scrollY = - mouseY + 12 + 4

            if (snapFlag) {
                if (width + 4 > screenWidth / scale) scrollX = - mouseX - 12 + 4
                if (height + 4 > screenHeight / scale) scrollY = - mouseY + 12 + 4
                snapFlag = false
            }

            x = ((mouseX + 12 + scrollX) / scale).toInt()
            y = ((mouseY - 12 + scrollY) / scale).toInt()

            drawFloatingRect(
                x - 4,
                y - 4,
                width + 8,
                height + 8,
                Color(25, 25, 25, 230)
            )

            var yStart = y
            for (textLine in textLines) {
                font.drawStringWithShadow(textLine, x.toFloat(), yStart.toFloat(), - 1)
                yStart += font.FONT_HEIGHT
            }

            GlStateManager.enableDepth()
            GlStateManager.popMatrix()
        }
        return true
    }

    @JvmStatic
    fun resetPos() {
        scrollX = 0
        scrollY = 0
        scaleScale = 0f
        snapFlag = true
    }
}