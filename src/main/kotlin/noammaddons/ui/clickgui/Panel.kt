package noammaddons.ui.clickgui

import noammaddons.features.Feature
import noammaddons.features.impl.gui.ConfigGui.accentColor
import noammaddons.noammaddons.Companion.textRenderer
import noammaddons.ui.clickgui.ClickGuiScreen.currentSettingMenu
import noammaddons.ui.clickgui.ClickGuiScreen.mc
import noammaddons.ui.clickgui.ClickGuiScreen.scale
import noammaddons.ui.clickgui.ClickGuiScreen.searchBar
import noammaddons.ui.clickgui.ClickGuiScreen.titleRenderer25
import noammaddons.ui.clickgui.core.*
import noammaddons.ui.config.core.CategoryType
import noammaddons.utils.MouseUtils.onMouseEnter
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderUtils.drawRect
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.StencilUtils
import noammaddons.utils.Utils.remove
import noammaddons.utils.Utils.removeSpace
import java.awt.Color
import kotlin.math.max

class Panel(val category: CategoryType, val features: MutableList<Feature>): AbstractElement() {
    var open = true
    val headerHeight = 20f
    val itemHeight = 20f
    var scrollY = 0f

    val menus = mutableMapOf<Feature, FeatureSettingMenu>()

    private val scrollbarWidth = 2f
    private val scrollbarMargin = 2f
    private val scrollbarMinHandleHeight = 20f

    init {
        this.width = 120f
        this.height = ((headerHeight + if (open) getFilteredFeatures().size * itemHeight else 0f).coerceAtMost(mc.getHeight().div(scale).times(0.8f))) + 10f
        this.y = 20f

        features.sortByDescending { textRenderer.getStringWidth(it.name.remove(" ").trim()) }
    }

    override fun draw(mouseX: Float, mouseY: Float) {
        val features = getFilteredFeatures().takeIf { it.isNotEmpty() } ?: return
        height = (headerHeight + if (open) getFilteredFeatures().size * itemHeight else 0f).coerceAtMost(mc.getHeight().div(scale).times(0.8f))

        drawRoundedRect(backgroundColor, x, y, width, height)

        titleRenderer25.drawCenteredText(
            "&l" + category.catName,
            x + width / 2,
            y + headerHeight / 2 - textRenderer.HALF_FONT_HEIGHT + 1,
            textColor
        )

        if (! open) return
        textRenderer.drawText(searchBar.value, 10, 10)

        StencilUtils.beginStencilClip {
            drawRect(Color(0), x, y + headerHeight, width, height - headerHeight - itemHeight)
            drawRoundedRect(Color(0), x, y + height - itemHeight, width, itemHeight)
        }

        var featureY = y + headerHeight - scrollY
        features.forEach { f ->
            if (searchBar.value.remove(" ").contains(f.name.removeSpace().trim(), true)) return@forEach

            onMouseEnter(mouseX, mouseY, x, featureY, width, itemHeight) {
                if (currentSettingMenu != null) return@onMouseEnter
                val actualItemWidth = if (isCurrentlyScrollable()) width - scrollbarWidth - (2 * scrollbarMargin) else width

                if (f == getFilteredFeatures().last()) {
                    drawRect(hoverColor, x, featureY, actualItemWidth, itemHeight - 10)
                    drawRoundedRect(hoverColor, x, featureY, actualItemWidth, itemHeight)
                }
                else drawRect(hoverColor, x, featureY, actualItemWidth, itemHeight)

            }

            textRenderer.drawCenteredText(
                (if (f.enabled) "&l" else "") + f.name.removeSpace().trim(),
                x + width / 2,
                featureY + itemHeight / 2 - textRenderer.HALF_FONT_HEIGHT + 1,
                if (f.enabled) accentColor else textColor
            )

            featureY += itemHeight
        }

        StencilUtils.endStencilClip()

        drawScrollbar()
    }

    private fun drawScrollbar() {
        if (isCurrentlyScrollable()) {
            val viewportY = y + headerHeight
            val viewportHeight = height - headerHeight
            val trackX = x + width - scrollbarWidth - scrollbarMargin
            val trackActualY = viewportY + scrollbarMargin
            val trackActualHeight = viewportHeight - (2 * scrollbarMargin)
            val totalContentHeight = getFilteredFeatures().size * itemHeight
            val maxScrollPossible = getMaxScroll()

            var handleRenderHeight = (viewportHeight / totalContentHeight) * trackActualHeight
            handleRenderHeight = handleRenderHeight.coerceIn(scrollbarMinHandleHeight, trackActualHeight)

            val scrollRatio = if (maxScrollPossible > 0f) scrollY / maxScrollPossible else 0f
            val handleAvailableTravel = trackActualHeight - handleRenderHeight
            val handleRenderY = trackActualY + (scrollRatio * handleAvailableTravel)
            val scrollbarHandleColor = accentColor

            drawRoundedRect(scrollbarHandleColor, trackX, handleRenderY, scrollbarWidth, handleRenderHeight, scrollbarWidth / 2f)
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int) {
        if (getFilteredFeatures().isEmpty()) return

        onMouseEnter(mouseX, mouseY, x, y, width, headerHeight) {
            if (button == 1) {
                open = ! open
                scrollY = 0f
            }
        }

        if (! open) return

        var featureY = y + headerHeight - scrollY
        getFilteredFeatures().forEach { f ->
            val itemClickableWidth = if (isCurrentlyScrollable()) width - scrollbarWidth - (2 * scrollbarMargin) else width
            onMouseEnter(mouseX, mouseY, x, featureY, itemClickableWidth, itemHeight) {
                if (button == 0) f.toggle()
                if (button == 1 && f.configSettings.isNotEmpty()) {
                    currentSettingMenu = menus.getOrPut(f) {
                        FeatureSettingMenu(f)
                    }
                }
            }

            featureY += itemHeight
        }
    }

    fun isCurrentlyScrollable(): Boolean {
        if (! open) return false
        val visibleContentAreaHeight = height - headerHeight
        return getFilteredFeatures().size * itemHeight > visibleContentAreaHeight
    }

    fun getMaxScroll(): Float {
        if (! open) return 0f
        val visibleContentAreaHeight = height - headerHeight
        val totalContentHeight = getFilteredFeatures().size * itemHeight
        return max(0f, totalContentHeight - visibleContentAreaHeight)
    }

    private fun getFilteredFeatures(): List<Feature> = features.filter {
        it.name.contains(searchBar.value, true)
    }
}