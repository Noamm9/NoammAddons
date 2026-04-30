package com.github.noamm9.ui.clickgui

import com.github.noamm9.features.Feature
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.features.impl.dev.ClickGui
import com.github.noamm9.features.impl.misc.sound.SoundManager
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class Panel(val category: CategoryType, var x: Int, var y: Int) {
    private val features = FeatureManager.getFeaturesByCategory(category)

    private val openAnim = Animation(150)
    var collapsed = false

    private val width = 110
    private val headerHeight = 22
    private val buttonHeight = 16

    private val maxDisplayHeight = 350
    private var scrollOffset = 0f

    private var dragging = false
    private var dragX = 0
    private var dragY = 0

    private val headerBg = Color(20, 20, 20, 230)
    private val bodyBg = Color(15, 15, 15, 180)
    private val hoverColor = Color(255, 255, 255, 30)

    fun render(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (dragging) {
            x = mouseX - dragX
            y = mouseY - dragY
        }

        val filteredFeatures = getSorting()
        if (filteredFeatures.isEmpty() && ClickGuiScreen.searchQuery.isNotEmpty()) return

        openAnim.update(if (collapsed && features.size == filteredFeatures.size) 0f else 1f)

        Render2D.drawRect(context, x, y, width, headerHeight, headerBg)
        Render2D.drawRect(context, x, y, width, 2, Style.accentColor)
        Render2D.drawCenteredString(context, "§l${if (category != CategoryType.FLOOR7) category.name else "Floor 7"}", x + width / 2, y + 7)

        if (openAnim.value > 0.01f || features.size != filteredFeatures.size) {
            val totalContentHeight = filteredFeatures.size * buttonHeight
            val visibleHeight = totalContentHeight.coerceAtMost(maxDisplayHeight)
            val currentScissorHeight = (visibleHeight * openAnim.value).toInt()
            val maxScroll = (totalContentHeight - visibleHeight).coerceAtLeast(0)
            if (scrollOffset > maxScroll) scrollOffset = maxScroll.toFloat()
            if (scrollOffset < 0) scrollOffset = 0f

            var currentY = y + headerHeight - scrollOffset.toInt()

            context.enableScissor(x, y + headerHeight, x + width, y + headerHeight + currentScissorHeight)

            filteredFeatures.forEach { feature ->
                if (currentY + buttonHeight > y + headerHeight && currentY < y + headerHeight + visibleHeight) {
                    val isHovered = mouseX >= x && mouseX <= x + width &&
                        mouseY >= currentY && mouseY <= currentY + buttonHeight &&
                        mouseY >= y + headerHeight && mouseY <= y + headerHeight + visibleHeight

                    Render2D.drawRect(context, x, currentY, width, buttonHeight, bodyBg)

                    if (feature.enabled) {
                        Render2D.drawRect(context, x, currentY, width, buttonHeight, Style.accentColor.withAlpha(100))
                        Render2D.drawRect(context, x, currentY, 2, buttonHeight, Style.accentColor)
                    }

                    if (isHovered) {
                        Render2D.drawRect(context, x, currentY, width, buttonHeight, hoverColor)
                    }

                    Render2D.drawCenteredString(context, feature.name, x + width / 2, currentY + 4)

                    if (isHovered && ! ClickGuiScreen.isMouseOverConfigWindow(mouseX, mouseY)) {
                        TooltipManager.hover(feature.description, mouseX, mouseY)
                    }
                }
                currentY += buttonHeight
            }
            context.disableScissor()

            if (maxScroll > 0 && ! collapsed) {
                val barHeight = (visibleHeight.toFloat() / totalContentHeight.toFloat()) * visibleHeight
                val barY = (y + headerHeight) + ((scrollOffset / maxScroll) * (visibleHeight - barHeight))

                Render2D.drawRect(context, x + width - 2, barY, 2, barHeight, Color.WHITE)
            }
        }
    }

    fun handleScroll(delta: Double) {
        if (collapsed) return
        val filteredFeatures = features.filter { it.name.contains(ClickGuiScreen.searchQuery, ignoreCase = true) }
        val totalContentHeight = filteredFeatures.size * buttonHeight
        val visibleHeight = totalContentHeight.coerceAtMost(maxDisplayHeight)

        if (totalContentHeight <= visibleHeight) return

        scrollOffset -= (delta * 15).toFloat()
    }

    fun isMouseOver(mx: Int, my: Int): Boolean {
        val filteredFeatures = getSorting()
        val totalContentHeight = filteredFeatures.size * buttonHeight
        val visibleHeight = totalContentHeight.coerceAtMost(maxDisplayHeight)

        return mx >= x && mx <= x + width && my >= y && my <= y + headerHeight + visibleHeight
    }

    fun isMouseOverHeader(mx: Double, my: Double): Boolean {
        return mx >= x && mx <= x + width && my >= y && my <= y + headerHeight
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

        val totalContentHeight = filteredFeatures.size * buttonHeight
        val visibleHeight = totalContentHeight.coerceAtMost(maxDisplayHeight)

        if (mouseY < y + headerHeight || mouseY > y + headerHeight + visibleHeight) return

        var currentY = y + headerHeight - scrollOffset.toInt()

        filteredFeatures.forEach { feature ->
            if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + buttonHeight) {
                if (button == 0) {
                    feature.toggle()
                    return
                }
                else if (button == 1 && feature.configSettings.isNotEmpty()) {
                    if (feature is SoundManager) {
                        ClickGuiScreen.selectedFeature = null
                        SoundManager.btn.action.invoke()
                    }
                    else ClickGuiScreen.openFeatureWindow(feature)
                    return
                }
            }
            currentY += buttonHeight
        }
    }

    fun mouseReleased(button: Int) {
        if (button == 0) dragging = false
    }

    private fun getSorting(): List<Feature> {
        val base = features.filter { it.name.contains(ClickGuiScreen.searchQuery, ignoreCase = true) }

        return when (ClickGui.panelSorting.value) {
            0 -> base.sortedBy { it.name }
            1 -> base.sortedByDescending { it.name.width() }
            else -> base
        }
    }
}