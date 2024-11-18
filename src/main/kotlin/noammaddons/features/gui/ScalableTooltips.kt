package noammaddons.features.gui

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderUtils.drawGradientRoundedRect
import noammaddons.utils.RenderUtils.drawRainbowRoundedBorder
import noammaddons.utils.Utils.isNull
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color


/**
 * Slightly modified code from DulkirMod
 * https://github.com/Noamm9/DulkirMod/blob/master/src/main/kotlin/dulkirmod/features/ScalableTooltips.kt
 *
 *  @see noammaddons.mixins.MixinGuiUtils
 */
object ScalableTooltips {
    private var scrollY: Int = 0
    private var scrollX: Int = 0
    private var snapFlag: Boolean = true
    private var scaleScale: Float = 0f
    private var lastItem: Int? = null

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
        val slot = (mc.currentScreen as? GuiContainer)?.slotUnderMouse
        if (lastItem.isNull()) lastItem = slot?.slotNumber
        else if (lastItem != slot?.slotNumber) {
            lastItem = slot?.slotNumber
            resetPos()
        }


        val eventDWheel = Mouse.getDWheel()
        if (mc.currentScreen !is GuiChat) {
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
            GlStateManager.scale(scale, scale, scale)
            GlStateManager.disableRescaleNormal()
            RenderHelper.disableStandardItemLighting()
            GlStateManager.disableLighting()
            GlStateManager.disableDepth()


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

            val backgroundColor = Color(33, 33, 33, 210)

            drawRainbowRoundedBorder(
                x - 3,
                y - 3,
                width + 6,
                height + 6,
                5f, 4f
            )

            drawGradientRoundedRect(
                x - 3f,
                y - 3f,
                width + 6f,
                height + 6f,
                5f,
                backgroundColor, backgroundColor,
                backgroundColor.darker(), backgroundColor.darker(),
            )

            var yStart = y
            for (textLine in textLines) {
                font.drawStringWithShadow(textLine, x.toFloat(), yStart.toFloat(), - 1)
                yStart += font.FONT_HEIGHT
            }

            GlStateManager.enableDepth()
            RenderHelper.enableStandardItemLighting()
            GlStateManager.enableRescaleNormal()
            GlStateManager.enableLighting()
            GlStateManager.popMatrix()
        }
        return true
    }

    fun resetPos() {
        scrollX = 0
        scrollY = 0
        scaleScale = 0f
        snapFlag = true
    }
}