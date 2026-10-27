package com.github.noamm9.ui.gui

import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.features.impl.dungeon.map.MapRenderer
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.TooltipManager
import com.github.noamm9.ui.clickgui.components.settings.*
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.ScrollState
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.scissor
import gg.essential.universal.UKeyboard
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.*
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.min

class DungeonMapScreen: Screen(Component.literal("DungeonMap")) {
    private companion object {
        private const val WINDOW_WIDTH = 560f
        private const val WINDOW_HEIGHT = 300f
        private const val SIDEBAR_WIDTH = 108f
        private const val PREVIEW_WIDTH = 160f
        private const val CATEGORY_HEIGHT = 20f
        private const val SETTING_SPACING = 5f
        private const val SCROLLBAR_WIDTH = 2f
    }

    private val categories = buildList<Category> {
        var current: Category? = null
        for (widget in MapConfig.configSettings.mapNotNull(WidgetFactory::fromSetting)) {
            val section = widget.config.section
            if (current == null || (section != null && section != current.name)) {
                current = Category(section ?: "General").also(::add)
            }
            current.widgets.add(widget)
        }
    }

    private var selectedCategory = categories.first()
    private var visibleWidgets = emptyList<Widget<*>>()
    private val scroll = ScrollState(minThumbHeight = 18f)

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.push(graphics)

        val mX = Resolution.getMouseX(mouseX)
        val mY = Resolution.getMouseY(mouseY)
        val x = windowX
        val y = windowY

        graphics.drawRect(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, Color(20, 20, 20, 240))
        graphics.drawRect(x, y, SIDEBAR_WIDTH, WINDOW_HEIGHT, Color(15, 15, 15, 200))
        graphics.drawRect(x + WINDOW_WIDTH - PREVIEW_WIDTH, y, PREVIEW_WIDTH, WINDOW_HEIGHT, Color(15, 15, 15, 200))
        graphics.drawRect(x, y, WINDOW_WIDTH, 2f, Style.accentColor)

        graphics.drawCenteredString("§lDungeon Map", x + SIDEBAR_WIDTH / 2, y + 10)
        graphics.drawCenteredString("§lPreview", x + WINDOW_WIDTH - PREVIEW_WIDTH / 2, y + 10)

        drawCategories(graphics, x, y, mX.toFloat(), mY.toFloat())
        drawSettings(graphics, mX, mY)
        drawPreview(graphics, x + WINDOW_WIDTH - PREVIEW_WIDTH, y + 26, PREVIEW_WIDTH, WINDOW_HEIGHT - 26)

        TooltipManager.draw(graphics, Resolution.width, Resolution.height)
        Resolution.pop(graphics)
    }

    private fun drawCategories(ctx: GuiGraphicsExtractor, x: Float, y: Float, mx: Float, my: Float) {
        shownCategories.forEachIndexed { index, category ->
            val catY = y + 30 + (index * CATEGORY_HEIGHT)
            val isHovered = mx >= x && mx <= x + SIDEBAR_WIDTH && my >= catY && my <= catY + CATEGORY_HEIGHT
            val isSelected = selectedCategory == category

            if (isSelected) ctx.drawRect(x, catY, SIDEBAR_WIDTH, CATEGORY_HEIGHT, Style.accentColor.withAlpha(40))
            if (isHovered && ! isSelected) ctx.drawRect(x, catY, SIDEBAR_WIDTH, CATEGORY_HEIGHT, Color(255, 255, 255, 10))

            val color = if (isSelected) Style.accentColor else Color.GRAY
            ctx.drawString(category.name, x + 10, catY + 6, color)
        }
    }

    private fun drawSettings(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (selectedCategory !in shownCategories) selectedCategory = shownCategories.firstOrNull() ?: categories.first()
        visibleWidgets = selectedCategory.widgets.filter { it.config.visibility() }

        val totalContentHeight = if (visibleWidgets.isEmpty()) 0f
        else visibleWidgets.sumOf { it.height }.toFloat() + ((visibleWidgets.size - 1) * SETTING_SPACING)

        scroll.update(totalContentHeight, listHeight)
        scroll.mouseDragged(mouseY.toFloat())

        val settingWidth = (listWidth - if (scroll.canScroll) 6f else 0f).toInt()

        ctx.scissor(listX, listY, listWidth, listHeight)

        var currentY = listY - scroll.value
        visibleWidgets.forEach { setting ->
            setting.x = listX.toInt()
            setting.y = currentY.toInt()
            setting.width = settingWidth

            setting.draw(ctx, mouseX, mouseY)

            val isHovered = mouseX >= setting.x && mouseX <= setting.x + setting.width &&
                mouseY >= setting.y && mouseY <= setting.y + setting.height &&
                isInsideList(mouseX.toFloat(), mouseY.toFloat())

            if (isHovered) TooltipManager.hover(setting.config.description, mouseX, mouseY)

            currentY += setting.height + SETTING_SPACING
        }

        ctx.disableScissor()

        scroll.drawScrollbar(ctx, listX + listWidth - SCROLLBAR_WIDTH, listY, SCROLLBAR_WIDTH, listHeight, mouseX.toFloat(), mouseY.toFloat())
    }

    private fun drawPreview(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float) {
        val mapHeight = if (MapConfig.mapExtraInfo.value) 140f else 128f
        val scale = min((w - 16f) / 128f, (h - 16f) / mapHeight)

        ctx.scissor(x, y, w, h)
        ctx.pose().pushMatrix()
        ctx.pose().translate(x + (w - 128f * scale) / 2f, y + 4f)
        ctx.pose().scale(scale)
        MapRenderer.draw(ctx, example = ! LocationUtils.inDungeon, extraInfo = true)
        ctx.pose().popMatrix()
        ctx.disableScissor()
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mx = Resolution.getMouseX(event.x).toFloat()
        val my = Resolution.getMouseY(event.y).toFloat()
        val button = event.button()

        if (button == 0 && mx >= windowX && mx <= windowX + SIDEBAR_WIDTH) shownCategories.forEachIndexed { index, category ->
            val catY = windowY + 30 + (index * CATEGORY_HEIGHT)
            if (my >= catY && my <= catY + CATEGORY_HEIGHT) {
                if (selectedCategory != category) {
                    selectedCategory = category
                    scroll.reset()
                    Style.playClickSound(1f)
                }
                return true
            }
        }

        if (button == 0 && scroll.mouseClicked(mx, my)) return true

        if (isInsideList(mx, my)) visibleWidgets.forEach {
            if (it.mouseClicked(mx.toDouble(), my.toDouble(), button)) return true
        }

        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0) scroll.mouseReleased()
        visibleWidgets.forEach { it.mouseReleased(event.button()) }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val mx = Resolution.getMouseX(mouseX)
        val my = Resolution.getMouseY(mouseY)

        visibleWidgets.forEach {
            if (it.mouseScrolled(mx, my, vertical)) return true
        }

        if (isInsideList(mx.toFloat(), my.toFloat())) scroll.scroll((vertical * 30).toFloat())
        return true
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        visibleWidgets.forEach {
            if (it.keyPressed(keyEvent.key, keyEvent.scancode, keyEvent.modifiers)) return true
        }

        if (keyEvent.key == UKeyboard.KEY_ESCAPE) {
            onClose()
            return true
        }

        return super.keyPressed(keyEvent)
    }

    override fun charTyped(e: CharacterEvent): Boolean {
        visibleWidgets.forEach {
            if (it.charTyped(e.codepoint.toChar())) return true
        }
        return super.charTyped(e)
    }

    override fun onClose() = GuiUtils.setScreen(ClickGuiScreen())

    private val shownCategories get() = categories.filter { category -> category.widgets.any { it.config.visibility() } }

    private val windowX get() = (Resolution.width / 2) - (WINDOW_WIDTH / 2)
    private val windowY get() = (Resolution.height / 2) - (WINDOW_HEIGHT / 2)
    private val listX get() = windowX + SIDEBAR_WIDTH + 10
    private val listY get() = windowY + 26
    private val listWidth get() = WINDOW_WIDTH - SIDEBAR_WIDTH - PREVIEW_WIDTH - 20
    private val listHeight get() = WINDOW_HEIGHT - 34

    private fun isInsideList(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight
    }

    private class Category(val name: String) {
        val widgets = mutableListOf<Widget<*>>()
    }
}
