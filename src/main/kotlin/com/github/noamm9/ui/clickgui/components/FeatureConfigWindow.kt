package com.github.noamm9.ui.clickgui.components

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.TooltipManager
import com.github.noamm9.ui.clickgui.components.settings.*
import com.github.noamm9.ui.clickgui.components.settings.impl.CategoryWidget
import com.github.noamm9.ui.clickgui.components.settings.impl.SeparatorWidget
import com.github.noamm9.ui.utils.ScrollState
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

open class FeatureConfigWindow(feature: Feature): FeatureWindow(feature) {
    private companion object {
        const val bottomPadding = 8f
        const val settingSpacing = 5f
        const val categoryWidth = 100f
        const val categoryHeight = 20f
    }

    override val defaultWidth get() = 250f + sidebarWidth + previewWidth
    override val defaultHeight = 260f
    override val minWidth get() = 180f + sidebarWidth + previewWidth
    override val minHeight = 140f

    protected open val previewWidth = 0f

    protected open val useSidebar = false

    private var visibleWidgets = emptyList<Widget<*>>()
    private val scroll = ScrollState(minThumbHeight = 18f)

    private val widgets = feature.configSettings.mapNotNull(WidgetFactory::fromSetting)

    val settings = buildList {
        var lastSection: String? = null

        for (widget in widgets) {
            val section = widget.config.section
            if (section != null && section != lastSection) {
                val sectionSettings = widgets.dropWhile { it !== widget }.takeWhile { it === widget || it.config.section == null }
                val sectionVisibility = { sectionSettings.any { it.config.visibility() } }
                if (isNotEmpty()) add(SeparatorWidget().also { it.config.visibility = sectionVisibility })
                add(CategoryWidget(section).also { it.config.visibility = sectionVisibility })
                lastSection = section
            }
            add(widget)
        }
    }

    private val categories = buildList<Category> {
        var current: Category? = null
        for (widget in widgets) {
            val section = widget.config.section
            if (current == null || (section != null && section != current.name)) {
                current = Category(section ?: "General").also(::add)
            }
            current.widgets.add(widget)
        }
    }

    private var selectedCategory = categories.firstOrNull()

    private var contentLeft = 0f
    private var contentTop = 0f
    private var contentRight = 0f
    private var contentBottom = 0f

    protected var previewX = 0f
        private set
    protected var previewY = 0f
        private set
    protected var previewHeight = 0f
        private set

    private var highlightedWidget: Widget<*>? = null
    private var highlightStart = 0L

    protected open fun drawPreview(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float) {}

    override fun drawContentBackground(context: GuiGraphicsExtractor) {
        if (useSidebar) context.drawRect(x, y + titleBarHeight, categoryWidth, height - titleBarHeight, Color(10, 10, 10, 160))
        if (previewWidth > 0f) drawBackground(context, previewX, previewY, previewWidth, previewHeight, Color(10, 10, 10, 160))
    }

    override fun drawContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (useSidebar) drawCategories(context, mouseX.toFloat(), mouseY.toFloat())
        drawSettings(context, mouseX, mouseY)

        if (previewWidth > 0f) {
            context.enableScissor(previewX.toInt(), previewY.toInt(), (previewX + previewWidth).toInt(), (previewY + previewHeight).toInt())
            drawPreview(context, previewX, previewY, previewWidth, previewHeight)
            context.disableScissor()
        }
    }

    private fun drawCategories(context: GuiGraphicsExtractor, mx: Float, my: Float) {
        shownCategories.forEachIndexed { index, category ->
            val catY = categoryY(index)
            val isHovered = mx >= x && mx <= x + categoryWidth && my >= catY && my <= catY + categoryHeight
            val isSelected = selectedCategory == category

            if (isSelected) context.drawRect(x, catY, categoryWidth, categoryHeight, Style.accentColor.withAlpha(40))
            if (isHovered && ! isSelected) context.drawRect(x, catY, categoryWidth, categoryHeight, Color(255, 255, 255, 10))

            val color = if (isSelected) Style.accentColor else Color.GRAY
            context.drawString(category.name, x + 10, catY + 6, color)
        }
    }

    private fun drawSettings(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (useSidebar && selectedCategory !in shownCategories) selectedCategory = shownCategories.firstOrNull() ?: categories.firstOrNull()
        visibleWidgets = currentWidgets.filter { it.config.visibility.invoke() }

        val viewportWidth = (contentRight - contentLeft).coerceAtLeast(120f)
        val viewportHeight = (contentBottom - contentTop).coerceAtLeast(40f)
        val totalContentHeight = if (visibleWidgets.isEmpty()) 0f
        else visibleWidgets.sumOf { it.height }.toFloat() + ((visibleWidgets.size - 1) * settingSpacing)

        scroll.update(totalContentHeight, viewportHeight)

        val scrollbarReserve = if (scroll.canScroll) 6f else 0f
        val settingWidth = (viewportWidth - scrollbarReserve).toInt().coerceAtLeast(120)

        context.enableScissor(contentLeft.toInt(), contentTop.toInt(), contentRight.toInt(), contentBottom.toInt())

        var currentY = contentTop - scroll.value

        visibleWidgets.forEach { setting ->
            setting.x = contentLeft.toInt()
            setting.y = currentY.toInt()
            setting.width = settingWidth

            setting.draw(context, mouseX, mouseY)

            val isHovered = mouseX >= setting.x && mouseX <= setting.x + setting.width &&
                mouseY >= setting.y && mouseY <= setting.y + setting.height &&
                isInsideContent(mouseX.toFloat(), mouseY.toFloat())

            if (isHovered) {
                TooltipManager.hover(setting.config.description, mouseX, mouseY)
            }

            currentY += setting.height + settingSpacing
        }

        highlightedWidget?.let { widget ->
            val age = System.currentTimeMillis() - highlightStart
            if (age > 1200) highlightedWidget = null
            else if (age > 0) {
                val alpha = (255 * (1f - age / 1200f)).toInt()
                context.drawBorder(widget.x, widget.y, widget.width, widget.height, Style.accentColor.withAlpha(alpha))
            }
        }

        context.disableScissor()

        scroll.drawScrollbar(context, contentRight - 2f, contentTop, 2f, viewportHeight, mouseX.toFloat(), mouseY.toFloat())
    }

    override fun onMouseClicked(mouseX: Float, mouseY: Float, button: Int) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && useSidebar && mouseX >= x && mouseX <= x + categoryWidth) {
            shownCategories.forEachIndexed { index, category ->
                val catY = categoryY(index)
                if (mouseY < catY || mouseY > catY + categoryHeight) return@forEachIndexed
                if (selectedCategory != category) {
                    clearFocus()
                    selectedCategory = category
                    scroll.reset()
                    Style.playClickSound(1f)
                }
                return
            }
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT && scroll.mouseClicked(mouseX, mouseY)) return clearFocus()

        if (isInsideContent(mouseX, mouseY)) {
            visibleWidgets.forEach { widget ->
                if (widget.mouseClicked(mouseX.toDouble(), mouseY.toDouble(), button)) {
                    settings.filter { it !== widget }.forEach { it.clearFocus() }
                    return
                }
            }
        }
    }

    override fun clearFocus() = settings.forEach { it.clearFocus() }

    override fun onMouseReleased(button: Int) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) scroll.mouseReleased()
        visibleWidgets.forEach { it.mouseReleased(button) }
    }

    override fun mouseScrolled(mouseX: Int, mouseY: Int, delta: Double): Boolean {
        visibleWidgets.forEach {
            if (it.mouseScrolled(mouseX, mouseY, delta)) {
                return true
            }
        }

        if (contains(mouseX.toFloat(), mouseY.toFloat())) {
            if (isInsideContent(mouseX.toFloat(), mouseY.toFloat())) {
                scroll.scroll((delta * 30).toFloat())
            }
            return true
        }

        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        visibleWidgets.forEach {
            if (it.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun charTyped(codePoint: Char): Boolean {
        visibleWidgets.forEach {
            if (it.charTyped(codePoint)) {
                return true
            }
        }
        return false
    }

    override fun updateLayout() {
        contentLeft = x + sidebarWidth + windowPadding
        contentTop = y + titleBarHeight + 8f
        contentRight = x + width - previewWidth - windowPadding
        contentBottom = y + height - bottomPadding

        previewX = x + width - previewWidth
        previewY = y + titleBarHeight
        previewHeight = height - titleBarHeight
    }

    override fun updateInteraction(mouseX: Float, mouseY: Float) {
        scroll.mouseDragged(mouseY)
        super.updateInteraction(mouseX, mouseY)
    }

    private fun isInsideContent(mouseX: Float, mouseY: Float): Boolean {
        return mouseX in contentLeft .. contentRight && mouseY >= contentTop && mouseY <= contentBottom
    }

    private val sidebarWidth get() = if (useSidebar) categoryWidth else 0f
    private val currentWidgets get() = if (useSidebar) selectedCategory?.widgets.orEmpty() else settings
    private val shownCategories get() = categories.filter { category -> category.widgets.any { it.config.visibility() } }

    private fun categoryY(index: Int) = y + titleBarHeight + 4f + (index * categoryHeight)

    private class Category(val name: String) {
        val widgets = mutableListOf<Widget<*>>()
    }
}