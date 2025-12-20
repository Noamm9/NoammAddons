package noammaddons.ui.clickgui

import kotlinx.coroutines.launch
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.scope
import noammaddons.NoammAddons.Companion.textRenderer
import noammaddons.features.Feature
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.ui.clickgui.ClickGuiScreen.titleRenderer25
import noammaddons.ui.clickgui.core.*
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.StencilUtils
import java.awt.Color
import kotlin.math.max

class FeatureSettingMenu(val feature: Feature): AbstractElement() {
    var scrollY = 0f

    init {
        val screenWidth = mc.getWidth() / ClickGuiScreen.scale
        val screenHeight = mc.getHeight() / ClickGuiScreen.scale
        this.width = 330f
        this.height = 350f
        this.x = (screenWidth - this.width) / 2
        this.y = (screenHeight - this.height) / 2
    }

    override fun draw(mouseX: Float, mouseY: Float) {
        drawRoundedRect(compBackgroundColor, x, y, width, height)

        drawRoundedRect(accentColor, x, y, width, 30f)
        drawRect(accentColor, x, y + 10, width, 30f - 10)

        titleRenderer25.drawCenteredText("&l" + feature.name, x + width / 2, y + 15 - titleRenderer25.HALF_FONT_HEIGHT, textColor)
        val descHight = textRenderer.drawWrappedText(feature.desc, x + 5, y + 35, width - 10, textColor, true).toFloat()

        StencilUtils.beginStencilClip {
            drawRect(Color(0), x + 5, y + 40 + descHight, width - 10, height - 40 - descHight)
        }

        var y = y + 40 + descHight - scrollY
        feature.configSettings.forEach { setting ->
            if (setting.hidden) return@forEach
            setting.width = (width - 10).toDouble()
            setting.draw(x.toDouble() + 5, y.toDouble(), mouseX.toDouble(), mouseY.toDouble())
            y += setting.height.toFloat() + 10f
            setting.width = 200.0
        }

        StencilUtils.endStencilClip()
        //  drawRoundedBorder(textColor, x, this.y, width, height, 5, 1.2)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int) {
        val descHight = textRenderer.warpText(feature.desc, width - 10).toFloat()
        if (!(mouseX in x..(x + width) && mouseY in y..(y + height)) && button == 0) {
            ClickGuiScreen.currentSettingMenu = null
            return
        }
        var y = y + 40 + descHight - scrollY



        feature.configSettings.forEach { setting ->
            if (setting.hidden) return@forEach
            setting.width = (width - 10).toDouble()
            setting.mouseClicked(x.toDouble() + 5, y.toDouble(), mouseX.toDouble(), mouseY.toDouble(), button)
            y += setting.height.toFloat() + 10f
            setting.width = 200.0
        }

        scope.launch {
            feature.configSettings.forEach { comp ->
                comp.updateVisibility()
            }
        }
    }

    override fun mouseDragged(mouseX: Float, mouseY: Float, button: Int) {
        val descHight = textRenderer.warpText(feature.desc, width - 10).toFloat()
        var y = y + 40 + descHight - scrollY

        feature.configSettings.forEach { setting ->
            if (setting.hidden) return@forEach
            setting.width = (width - 10).toDouble()
            setting.mouseDragged(x.toDouble() + 5, y.toDouble(), mouseX.toDouble(), mouseY.toDouble(), button)
            y += setting.height.toFloat() + 10f
            setting.width = 200.0
        }
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        val descHight = textRenderer.warpText(feature.desc, width - 10).toFloat()
        var y = y + 40 + descHight - scrollY

        feature.configSettings.forEach { setting ->
            if (setting.hidden) return@forEach
            setting.width = (width - 10).toDouble()
            setting.mouseRelease(x.toDouble() + 5, y.toDouble(), mouseX.toDouble(), mouseY.toDouble(), button)
            y += setting.height.toFloat() + 10f
            setting.width = 200.0
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        var b = false

        feature.configSettings.forEach {
            if (it.hidden) return@forEach
            if (! it.keyTyped(typedChar, keyCode)) return@forEach
            b = true
        }

        return b
    }

    fun isCurrentlyScrollable(): Boolean {
        val descHight = textRenderer.warpText(feature.desc, width - 10.0)
        val maxCompsHight = feature.configSettings.filterNot { it.hidden }.sumOf { it.height + 10f }.toFloat()

        return maxCompsHight + descHight + 40 > (height - 30f)
    }

    fun getMaxScroll(): Float {
        val visibleContentAreaHeight = height - 30f
        val descHight = textRenderer.warpText(feature.desc, width - 10.0).toFloat()
        val maxCompsHight = feature.configSettings.filterNot { it.hidden }.sumOf { it.height + 10f }.toFloat()

        return max(0f, maxCompsHight + descHight - visibleContentAreaHeight) + 10
    }
}