package noammaddons.features.impl.gui

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.client.config.GuiUtils
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ItemUtils
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.roundToInt


/**
 *  @see noammaddons.mixins.MixinGuiUtils.drawScaledHoveringText
 */
object ScalableTooltips: Feature("Allows you to scroll and scale tooltips") {
    private var scrollY: Int = 0
    private var scrollX: Int = 0
    private var snapFlag: Boolean = true
    private var scaleScale: Float = 0f
    private var lastShit: Slot? = null

    private val tooltipScale by SliderSetting("Scale", 1, 100, 80)
    private val rarityBorder by ToggleSetting("Rarity Border Color")

    private val backgroundColor by ColorSetting("Background Color", Color(16, 0, 16, 240))
    private val borderColor by ColorSetting("Border Color", Color(80, 0, 255, 80))
        .addDependency(getSettingByName("Rarity Border Color") as ToggleSetting) {
            it.value
        }

    @JvmStatic
    fun drawScaledHoveringText(
        textLines: List<String>,
        mouseX: Int,
        mouseY: Int,
        screenWidth: Int,
        screenHeight: Int,
        font: FontRenderer,
    ): Boolean {
        if (! enabled) return false
        if (textLines.isEmpty()) return true
        val gui = mc.currentScreen ?: return false
        val slot = (gui as? GuiContainer)?.slotUnderMouse
        if (lastShit == null) lastShit = slot
        else if (lastShit != slot) {
            lastShit = slot
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
            val scale = (((3f * (tooltipScale.toFloat() / 100)) + scaleScale) / mc.getScaleFactor()).coerceAtLeast(1.5f / mc.getScaleFactor())
            val width = textLines.maxOf { getStringWidth(it.trim()).roundToInt() }
            val height = (textLines.size) * 9

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


            val zLevel = 300
            val backgroundColor = backgroundColor.rgb
            val borderColorStart = if (! rarityBorder) borderColor.rgb else ItemUtils.getRarity(slot?.stack).color.rgb
            val borderColorEnd = (if (! rarityBorder) borderColor else ItemUtils.getRarity(slot?.stack).color).darker().darker().rgb

            val firstLineOffset = - 2
            val adjustedYOffset = y + firstLineOffset
            val totalHeight = height - firstLineOffset

            GuiUtils.drawGradientRect(zLevel, x - 3, adjustedYOffset - 4, x + width + 3, adjustedYOffset - 3, backgroundColor, backgroundColor)
            GuiUtils.drawGradientRect(zLevel, x - 3, adjustedYOffset + totalHeight + 3, x + width + 3, adjustedYOffset + totalHeight + 4, backgroundColor, backgroundColor)
            GuiUtils.drawGradientRect(zLevel, x - 3, adjustedYOffset - 3, x + width + 3, adjustedYOffset + totalHeight + 3, backgroundColor, backgroundColor)
            GuiUtils.drawGradientRect(zLevel, x - 4, adjustedYOffset - 3, x - 3, adjustedYOffset + totalHeight + 3, backgroundColor, backgroundColor)
            GuiUtils.drawGradientRect(zLevel, x + width + 3, adjustedYOffset - 3, x + width + 4, adjustedYOffset + totalHeight + 3, backgroundColor, backgroundColor)

            GuiUtils.drawGradientRect(zLevel, x - 3, adjustedYOffset - 2, x - 2, adjustedYOffset + totalHeight + 2, borderColorStart, borderColorEnd)
            GuiUtils.drawGradientRect(zLevel, x + width + 2, adjustedYOffset - 2, x + width + 3, adjustedYOffset + totalHeight + 2, borderColorStart, borderColorEnd)
            GuiUtils.drawGradientRect(zLevel, x - 3, adjustedYOffset - 3, x + width + 3, adjustedYOffset - 2, borderColorStart, borderColorStart)
            GuiUtils.drawGradientRect(zLevel, x - 3, adjustedYOffset + totalHeight + 2, x + width + 3, adjustedYOffset + totalHeight + 3, borderColorEnd, borderColorEnd)

            var yStart = y
            for ((i, textLine) in textLines.withIndex()) {
                val yOffset = if (i == 0) firstLineOffset else 0
                font.drawStringWithShadow(textLine, x.toFloat(), (yStart + yOffset).toFloat(), - 1)
                yStart += 9
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