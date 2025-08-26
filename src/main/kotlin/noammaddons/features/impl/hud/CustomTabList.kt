package noammaddons.features.impl.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlayNoCaching
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawRainbowRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.TablistUtils.getTabListFooterText
import noammaddons.utils.TablistUtils.tabList
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.splitArray
import java.awt.Color

object CustomTabList: Feature() {
    private val scale by SliderSetting("Scale", 1, 100, 1, 75)

    @SubscribeEvent
    fun onRenderOverlayNoCaching(event: RenderOverlayNoCaching) {
        if (! mc.gameSettings.keyBindPlayerList.isKeyDown) return
        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, 0f, 300f)
        drawTablist()
        GlStateManager.popMatrix()
    }

    fun drawTablist() {
        val scale = (2.4f * (scale.toFloat() / 100f)) / mc.getScaleFactor()
        val screenWidth = mc.getWidth() / scale
        val screenHeight = mc.getHeight() / scale
        val fontHeight = 9

        val names = tabList.map { it.second }.takeUnless { it.isEmpty() } ?: return
        val footerLines = getTabListFooterText()?.split("\n")?.toMutableList()?.apply {
            removeIf { it.removeFormatting().contains("hypixel.net", true) }
        }?.takeUnless { it.isEmpty() }

        val maxNameWidth = names.maxOfOrNull { getStringWidth(it) } ?: return
        val maxFooterWidth = footerLines.takeUnless { it?.isEmpty() == true }?.maxOfOrNull { getStringWidth(it) } ?: 0f

        val rowsCount = splitArray(names, 20).size.coerceIn(1, 4)

        val tableWidth = (maxNameWidth + 20) * rowsCount
        val tableHeight = fontHeight * 25

        val xOffset = (screenWidth - tableWidth) / 2
        val yOffset = screenHeight / 20

        GlStateManager.scale(scale, scale, scale)

        drawRoundedRect(Color(33, 33, 33, 153), xOffset, yOffset, tableWidth, tableHeight)
        drawRainbowRoundedBorder(xOffset, yOffset, tableWidth, tableHeight)

        splitArray(names, 20).forEachIndexed { index, row ->
            if (index >= 4) return@forEachIndexed

            row.forEachIndexed { i, line ->
                drawText(
                    line,
                    xOffset + ((maxNameWidth + 20f) * index) + 10,
                    yOffset + fontHeight * ((i + 1) * 1.15f)
                )
            }
        }

        if (footerLines == null) return
        if (footerLines.isNotEmpty() && footerLines.joinToString { it.removeFormatting().lowercase().remove(Regex("\\s")) }.isNotBlank()) {
            val footerWidth = maxFooterWidth + 20
            val footerHeight = fontHeight * footerLines.size + 10
            val footerXOffset = screenWidth / 2 - footerWidth / 2
            val footerYOffset = yOffset + tableHeight + 30

            drawRoundedRect(Color(33, 33, 33, 153), footerXOffset, footerYOffset, footerWidth, footerHeight)
            drawRainbowRoundedBorder(footerXOffset, footerYOffset, footerWidth, footerHeight)

            footerLines.forEachIndexed { i, line ->
                drawText(
                    line,
                    footerXOffset + (footerWidth - getStringWidth(line)) / 2f,
                    footerYOffset + fontHeight * (i + 1f) - 5
                )
            }
        }
    }
}
