package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.features.Feature
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.features.impl.general.CommandShortcuts
import com.github.noamm9.features.impl.misc.sound.SoundManager
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.SuggestionManager
import com.github.noamm9.ui.clickgui.TooltipManager
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.gui.CommandShortcutsScreen
import com.github.noamm9.ui.gui.SoundManagerScreen
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.scissor
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class CategoryPanel(val category: CategoryType, var x: Int, var y: Int, private val screen: ClickGuiScreen) {
    private companion object {
        const val WIDTH = 110
        const val HEADER_HEIGHT = 22
        const val BUTTON_HEIGHT = 16
        const val MAX_DISPLAY_HEIGHT = 350

        private val headerBg = Color(20, 20, 20, 230)
        private val bodyBg = Color(15, 15, 15, 180)
        private val hoverColor = Color(255, 255, 255, 30)
    }

    private val features = FeatureManager.getFeaturesByCategory(category)

    private val openAnim = Animation(150)
    var collapsed = false

    private var scrollOffset = 0f
    var dragging = false
    private var dragX = 0
    private var dragY = 0

    fun render(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (dragging) {
            x = mouseX - dragX
            y = mouseY - dragY
        }

        val filteredFeatures = getSorting()
        if (filteredFeatures.isEmpty() && screen.searchQuery.isNotEmpty()) return

        openAnim.update(if (collapsed && features.size == filteredFeatures.size) 0f else 1f)

        context.drawRect(x, y, WIDTH, HEADER_HEIGHT, headerBg)
        context.drawRect(x, y, WIDTH, 2, Style.accentColor)
        context.drawCenteredString("§l${if (category != CategoryType.FLOOR7) category.name else "Floor 7"}", x + WIDTH / 2, y + 7)

        if (openAnim.value > 0.01f || features.size != filteredFeatures.size) {
            val totalContentHeight = filteredFeatures.size * BUTTON_HEIGHT
            val visibleHeight = totalContentHeight.coerceAtMost(MAX_DISPLAY_HEIGHT)
            val currentScissorHeight = (visibleHeight * openAnim.value).toInt()
            val maxScroll = (totalContentHeight - visibleHeight).coerceAtLeast(0)
            if (scrollOffset > maxScroll) scrollOffset = maxScroll.toFloat()
            if (scrollOffset < 0) scrollOffset = 0f

            var currentY = y + HEADER_HEIGHT - scrollOffset.toInt()

            context.scissor(x, y + HEADER_HEIGHT, WIDTH, currentScissorHeight)

            filteredFeatures.forEach { feature ->
                if (currentY + BUTTON_HEIGHT > y + HEADER_HEIGHT && currentY < y + HEADER_HEIGHT + visibleHeight) {
                    val isHovered = mouseX >= x && mouseX <= x + WIDTH &&
                        mouseY >= currentY && mouseY <= currentY + BUTTON_HEIGHT &&
                        mouseY >= y + HEADER_HEIGHT && mouseY <= y + HEADER_HEIGHT + visibleHeight

                    context.drawRect(x, currentY, WIDTH, BUTTON_HEIGHT, bodyBg)

                    if (feature.enabled) {
                        context.drawRect(x, currentY, WIDTH, BUTTON_HEIGHT, Style.accentColor.withAlpha(100))
                        context.drawRect(x, currentY, 2, BUTTON_HEIGHT, Style.accentColor)
                    }

                    if (isHovered) {
                        context.drawRect(x, currentY, WIDTH, BUTTON_HEIGHT, hoverColor)
                    }

                    context.drawCenteredString(feature.name, x + WIDTH / 2, currentY + 4)

                    if (isHovered && ! screen.isMouseOverConfigWindow(mouseX, mouseY)) {
                        TooltipManager.hover(feature.description, mouseX, mouseY)
                    }
                }
                currentY += BUTTON_HEIGHT
            }
            context.disableScissor()

            if (maxScroll > 0 && ! collapsed) {
                val barHeight = (visibleHeight.toFloat() / totalContentHeight.toFloat()) * visibleHeight
                val barY = (y + HEADER_HEIGHT) + ((scrollOffset / maxScroll) * (visibleHeight - barHeight))

                context.drawRect(x + WIDTH - 2, barY, 2, barHeight)
            }
        }
    }

    fun handleScroll(delta: Double) {
        if (collapsed) return
        val filteredFeatures = features.filter { it.name.contains(screen.searchQuery, ignoreCase = true) }
        val totalContentHeight = filteredFeatures.size * BUTTON_HEIGHT
        val visibleHeight = totalContentHeight.coerceAtMost(MAX_DISPLAY_HEIGHT)

        if (totalContentHeight <= visibleHeight) return

        scrollOffset -= (delta * 15).toFloat()
    }

    fun isMouseOver(mx: Int, my: Int): Boolean {
        val filteredFeatures = getSorting()
        val totalContentHeight = filteredFeatures.size * BUTTON_HEIGHT
        val visibleHeight = totalContentHeight.coerceAtMost(MAX_DISPLAY_HEIGHT)

        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + HEADER_HEIGHT + visibleHeight
    }

    fun isMouseOverHeader(mx: Double, my: Double): Boolean {
        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + HEADER_HEIGHT
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) {
        if (isMouseOverHeader(mouseX, mouseY)) {
            if (button == 0) {
                dragging = true
                dragX = (mouseX - x).toInt()
                dragY = (mouseY - y).toInt()
            }
            else if (button == 1) {
                collapsed = ! collapsed
                Style.playClickSound(if (collapsed) 0.8f else 1.1f)
            }
            return
        }

        val filteredFeatures = getSorting()
        if (collapsed && features.size == filteredFeatures.size) return

        val totalContentHeight = filteredFeatures.size * BUTTON_HEIGHT
        val visibleHeight = totalContentHeight.coerceAtMost(MAX_DISPLAY_HEIGHT)

        if (mouseY < y + HEADER_HEIGHT || mouseY > y + HEADER_HEIGHT + visibleHeight) return

        var currentY = y + HEADER_HEIGHT - scrollOffset.toInt()

        filteredFeatures.forEach { feature ->
            if (mouseX >= x && mouseX <= x + WIDTH && mouseY >= currentY && mouseY <= currentY + BUTTON_HEIGHT) {
                if (button == 0) {
                    feature.toggle()
                    return
                }
                else if (button == 1) {
                    openFeature(feature)
                    return
                }
            }
            currentY += BUTTON_HEIGHT
        }
    }

    fun mouseReleased(button: Int) {
        if (button == 0) dragging = false
    }

    private fun getSorting(): Collection<Feature> {
        val suggestions = if (screen.searchQuery.isBlank()) features
        else SuggestionManager.getSuggestions(screen.searchQuery, features)

        return when (ClickGui.panelSorting.value) {
            0 -> suggestions.sortedBy { it.name }
            1 -> suggestions.sortedByDescending { it.name.width() }
            else -> suggestions
        }
    }

    fun openFeature(feature: Feature) {
        if (feature is SoundManager || feature is CommandShortcuts) {
            val screen = if (feature is SoundManager) SoundManagerScreen() else CommandShortcutsScreen()
            if (feature.enabled) GuiUtils.setScreen(screen)
            else NotificationManager.push("$MOD_NAME - ClickGui", "&fEnable &b${feature.name} &ffirst to open the settings!")
        }
        else if (feature.configSettings.isNotEmpty()) screen.openFeatureWindow(feature)
    }
}