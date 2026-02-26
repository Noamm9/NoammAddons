package com.github.noamm9.ui.clickgui

import com.github.noamm9.config.Config
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color


object ClickGuiScreen: Screen(Component.literal("ClickGUI")) {
    private val panels = mutableListOf<Panel>()
    var searchQuery = ""

    private val searchHandler = TextInputHandler(
        textProvider = { searchQuery },
        textSetter = { searchQuery = it }
    )

    var selectedFeature: Feature? = null
    private var scrollTarget = 0f
    private val scrollAnim = Animation(200L)

    init {
        CategoryType.entries.forEachIndexed { index, category ->
            panels.add(Panel(category, 20 + (index * 120), 20))
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        Resolution.refresh()
        Resolution.push(context)
        val mX = Resolution.getMouseX(mouseX.toDouble())
        val mY = Resolution.getMouseY(mouseY.toDouble())

        TooltipManager.reset()
        context.fillGradient(0, 0, Resolution.width.toInt(), Resolution.height.toInt(), Color(0, 0, 0, 100).rgb, Color(0, 0, 0, 150).rgb)

        panels.forEach { it.render(context, mX, mY) }

        drawSearchBar(context, mX.toFloat(), mY.toFloat())

        selectedFeature?.let { feature ->
            drawSettingsMenu(context, feature, mX, mY)
        }

        TooltipManager.draw(context, Resolution.width, Resolution.height)
        Resolution.pop(context)
    }

    private fun drawSettingsMenu(context: GuiGraphics, feature: Feature, mx: Int, my: Int) {
        val x = (Resolution.width / 2) - 100
        val y = (Resolution.height / 2) - 125
        val menuWidth = 200
        val menuHeight = 250

        Render2D.drawRect(context, x, y, menuWidth.toFloat(), menuHeight.toFloat(), Color(20, 20, 20, 240))
        Render2D.drawRect(context, x, y, menuWidth.toFloat(), 2f, Style.accentColor)
        Render2D.drawCenteredString(context, "§l${feature.name}", Resolution.width / 2, y + 10)
        Render2D.drawRect(context, x + 10, y + 28, 180f, 1f, Color(255, 255, 255, 30))

        val visibleSettings = feature.configSettings.filter { it.isVisible }

        val totalContentHeight = visibleSettings.sumOf { it.height + 5 }.toFloat() + 10
        val viewportHeight = 195f
        val maxScroll = if (totalContentHeight > viewportHeight) totalContentHeight - viewportHeight else 0f

        context.enableScissor(x.toInt(), (y + 30).toInt(), (x + menuWidth).toInt(), (y + 30 + 210).toInt())

        scrollAnim.update(scrollTarget)
        var sY = y + 40 + scrollAnim.value

        visibleSettings.forEach { setting ->
            setting.x = (Resolution.width / 2).toInt() - 90
            setting.y = sY.toInt()
            setting.width = 180

            setting.draw(context, mx, my)

            val isHovered = mx >= setting.x && mx <= setting.x + setting.width &&
                my >= setting.y && my <= setting.y + setting.height &&
                my > y + 30 && my < y + 225

            if (isHovered) TooltipManager.hover(setting.description, mx, my)

            sY += setting.height + 5
        }
        context.disableScissor()

        if (maxScroll > 0) {
            val barWidth = 2f
            val barX = x + menuWidth - barWidth - 2f
            val barY = y + 32f
            val barHeight = viewportHeight - 4f

            Render2D.drawRect(context, barX, barY, barWidth, barHeight, Color(255, 255, 255, 15))

            val thumbHeight = (viewportHeight / totalContentHeight) * barHeight
            val thumbY = barY + (- scrollAnim.value / totalContentHeight) * barHeight

            Render2D.drawRect(context, barX, thumbY, barWidth, thumbHeight, Style.accentColor.withAlpha(160))
        }

        if (scrollTarget > 0) scrollTarget = 0f
        if (scrollTarget < - maxScroll) scrollTarget = - maxScroll
    }

    private fun drawSearchBar(context: GuiGraphics, mX: Float, mY: Float) {
        val bw = 150f
        val bh = 22f
        val bx = (Resolution.width / 2) - (bw / 2)
        val by = Resolution.height - 40

        Render2D.drawRect(context, bx, by, bw, bh, Color(15, 15, 15, 200))

        val borderColor = if (searchHandler.listening) Style.accentColor else Color(255, 255, 255, 30)
        Render2D.drawRect(context, bx, by + bh - 2, bw, 2f, borderColor)

        searchHandler.x = bx
        searchHandler.y = by
        searchHandler.width = bw
        searchHandler.height = bh

        if (searchQuery.isEmpty() && ! searchHandler.listening) {
            Render2D.drawCenteredString(context, "§8Search...", Resolution.width / 2, by + 7, Color.GRAY, shadow = false)
        }
        else searchHandler.draw(context, mX, mY)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = Resolution.getMouseX(mouseButtonEvent.x)
        val my = Resolution.getMouseY(mouseButtonEvent.y)
        val button = mouseButtonEvent.button()

        selectedFeature?.let { feature ->
            val menuX = (Resolution.width / 2) - 100
            val menuY = (Resolution.height / 2) - 125
            if (mx < menuX || mx > menuX + 200 || my < menuY || my > menuY + 250) {
                selectFeature(null)
                return true
            }
            if (my > menuY + 30 && my < menuY + 225) {
                feature.configSettings.forEach {
                    if (! it.isVisible) return@forEach
                    if (it.mouseClicked(mx.toDouble(), my.toDouble(), button)) return true
                }
            }
            return true
        }

        panels.asReversed().find { panel -> panel.isMouseOverHeader(mx.toDouble(), my.toDouble()) }?.let { clickedPanel ->
            panels.remove(clickedPanel)
            panels.add(clickedPanel)

            clickedPanel.mouseClicked(mx.toDouble(), my.toDouble(), button)
            searchHandler.listening = false

            return true
        }

        if (searchHandler.mouseClicked(mx.toFloat(), my.toFloat(), mouseButtonEvent)) {
            return true
        }

        panels.forEach { it.mouseClicked(mx.toDouble(), my.toDouble(), button) }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        val mx = Resolution.getMouseX(mouseButtonEvent.x)
        val my = Resolution.getMouseY(mouseButtonEvent.y)
        searchHandler.mouseReleased()
        selectedFeature?.configSettings?.forEach { it.mouseReleased(mouseButtonEvent.button()) }
        panels.forEach { it.mouseReleased(mx.toDouble(), my.toDouble(), mouseButtonEvent.button()) }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val mx = Resolution.getMouseX(mouseX)
        val my = Resolution.getMouseY(mouseY)

        if (selectedFeature != null) {
            selectedFeature?.configSettings?.forEach {
                if (it.mouseScrolled(mx, my, vertical)) return true
            }

            scrollTarget += (vertical * 30).toFloat()
            return true
        }

        panels.asReversed().find { it.isMouseOver(mx, my) }?.let { panel ->
            panel.handleScroll(vertical)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        selectedFeature?.configSettings?.forEach {
            if (it.isVisible && it.charTyped(characterEvent.codepoint.toChar(), characterEvent.modifiers)) {
                return true
            }
        }

        if (searchHandler.keyTyped(characterEvent)) return true
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (selectedFeature != null) {
            selectedFeature?.configSettings?.forEach {
                if (it.isVisible && it.keyPressed(keyEvent.key, keyEvent.scancode, keyEvent.modifiers)) {
                    return true
                }
            }

            if (keyEvent.key == InputConstants.KEY_ESCAPE) {
                if (selectedFeature != null) {
                    selectedFeature = null
                    return true
                }
            }
        }

        if (searchHandler.keyPressed(keyEvent)) return true
        if (keyEvent.hasControlDown() && keyEvent.input() == GLFW.GLFW_KEY_F) {
            searchHandler.listening = ! searchHandler.listening
            return true
        }

        return super.keyPressed(keyEvent)
    }

    fun selectFeature(feature: Feature?) {
        selectedFeature = feature
        scrollTarget = 0f
        scrollAnim.set(0f)
    }

    override fun onClose() {
        selectFeature(null)
        searchHandler.listening = false
        Config.save()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}