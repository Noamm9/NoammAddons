package noammaddons.features.hud

import net.minecraft.client.renderer.GlStateManager.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.renderPlayerlist
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawRainbowRoundedBorder
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.TablistUtils.getTabList
import noammaddons.utils.TablistUtils.getTabListFooterText
import noammaddons.utils.Utils.splitArray
import java.awt.Color

object CustomTabList: Feature() {
    private val backgroundColor = Color(33, 33, 33, 153)

    @SubscribeEvent
    fun drawCustomTabList(event: renderPlayerlist) {
        if (! config.CustomTabList) return
        event.isCanceled = true

        val scale = (2.4f * config.CustomTabListScale) / mc.getScaleFactor()

        val screenWidth = mc.getWidth() / scale
        val screenHeight = mc.getHeight() / scale
        val fontHeight = 9

        val names = getTabList.map { it.second }
        val footerLines = getTabListFooterText()?.split("\n")?.toMutableList()?.apply {
            removeIf { it.removeFormatting().contains("hypixel.net", true) }
        } ?: emptyList()

        var maxNameWidth = 0
        var maxFooterWidth = 0

        names.forEach { maxNameWidth = maxNameWidth.coerceAtLeast(getStringWidth(it.removeFormatting()).toInt()) }
        footerLines.forEach { maxFooterWidth = maxOf(maxFooterWidth, getStringWidth(it.removeFormatting()).toInt()) }

        val rowsCount = splitArray(names, 20).size.coerceIn(1, 4)

        val tableWidth = (maxNameWidth + 20) * rowsCount
        val tableHeight = fontHeight * 25

        val xOffset = (screenWidth - tableWidth) / 2
        val yOffset = screenHeight / 20

        pushMatrix()
        scale(scale, scale, scale)

        drawRoundedRect(
            backgroundColor,
            xOffset, yOffset,
            tableWidth, tableHeight
        )

        drawRainbowRoundedBorder(
            xOffset, yOffset,
            tableWidth, tableHeight
        )

        // Draw the player names
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

        if (footerLines.isNotEmpty() && footerLines.joinToString {
                it.removeFormatting().lowercase().replace(Regex("\\s"), "")
            } != "") {
            val footerWidth = maxFooterWidth + 20
            val footerHeight = fontHeight * footerLines.size + 10

            val footerXOffset = screenWidth / 2 - footerWidth / 2
            val footerYOffset = yOffset + tableHeight + 30

            drawRoundedRect(
                backgroundColor,
                footerXOffset, footerYOffset,
                footerWidth, footerHeight
            )

            drawRainbowRoundedBorder(
                footerXOffset, footerYOffset,
                footerWidth, footerHeight
            )

            footerLines.forEachIndexed { i, line ->
                drawText(
                    line,
                    footerXOffset + (footerWidth - getStringWidth(line)) / 2f,
                    footerYOffset + fontHeight * (i + 1f) - 5
                )
            }
        }
        popMatrix()
    }
}
