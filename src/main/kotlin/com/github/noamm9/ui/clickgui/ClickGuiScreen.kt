package com.github.noamm9.ui.clickgui

import com.github.noamm9.config.Config
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.clickgui.enums.WindowClickAction
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
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
    private const val defaultWindowWidth = 220f
    private const val defaultWindowHeight = 260f
    private const val windowCascadeOffset = 18f

    private val panels = mutableListOf<Panel>()
    private val configWindows = mutableListOf<FeatureConfigWindow>()
    var searchQuery = ""

    private val searchHandler = TextInputHandler(
        textProvider = { searchQuery },
        textSetter = { searchQuery = it }
    )

    var selectedFeature: Feature?
        get() = configWindows.lastOrNull()?.feature
        set(value) {
            if (value == null) closeAllFeatureWindows()
            else openFeatureWindow(value)
        }

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
        configWindows.forEachIndexed { index, window ->
            window.render(context, mX, mY, index == configWindows.lastIndex)
        }

        TooltipManager.draw(context, Resolution.width, Resolution.height)
        Resolution.pop(context)
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

        configWindows.asReversed().find { it.contains(mx.toFloat(), my.toFloat()) }?.let { window ->
            focusWindow(window)
            blurAllWindows(window)
            searchHandler.listening = false

            when (window.mouseClicked(mx, my, button)) {
                WindowClickAction.CLOSE -> closeWindow(window)
                WindowClickAction.CONSUMED -> {}
            }
            return true
        }

        blurAllWindows()

        panels.asReversed().find { it.isMouseOverHeader(mx.toDouble(), my.toDouble()) }?.let { clickedPanel ->
            panels.remove(clickedPanel)
            panels.add(clickedPanel)

            clickedPanel.mouseClicked(mx.toDouble(), my.toDouble(), button)
            searchHandler.listening = false
            return true
        }

        if (searchHandler.mouseClicked(mx.toFloat(), my.toFloat(), mouseButtonEvent)) {
            return true
        }

        panels.asReversed().find { it.isMouseOver(mx, my) }?.let { panel ->
            panel.mouseClicked(mx.toDouble(), my.toDouble(), button)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        val mx = Resolution.getMouseX(mouseButtonEvent.x)
        val my = Resolution.getMouseY(mouseButtonEvent.y)
        val button = mouseButtonEvent.button()

        searchHandler.mouseReleased()
        configWindows.forEach { it.mouseReleased(button) }
        panels.forEach { it.mouseReleased(button) }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val mx = Resolution.getMouseX(mouseX)
        val my = Resolution.getMouseY(mouseY)

        configWindows.asReversed().find { it.contains(mx.toFloat(), my.toFloat()) }?.let { window ->
            focusWindow(window)
            window.mouseScrolled(mx, my, vertical)
            return true
        }

        panels.asReversed().find { it.isMouseOver(mx, my) }?.let { panel ->
            panel.handleScroll(vertical)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        configWindows.lastOrNull()?.let { window ->
            if (window.charTyped(characterEvent.codepoint.toChar(), characterEvent.modifiers)) {
                return true
            }
        }

        if (searchHandler.keyTyped(characterEvent)) return true
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        configWindows.lastOrNull()?.let { window ->
            if (window.keyPressed(keyEvent.key, keyEvent.scancode, keyEvent.modifiers)) {
                return true
            }

            if (keyEvent.key == InputConstants.KEY_ESCAPE) {
                closeWindow(window)
                return true
            }
        }

        if (searchHandler.keyPressed(keyEvent)) return true
        if (keyEvent.hasControlDown() && keyEvent.input() == GLFW.GLFW_KEY_F) {
            searchHandler.listening = ! searchHandler.listening
            return true
        }

        return super.keyPressed(keyEvent)
    }

    fun openFeatureWindow(feature: Feature, preferredX: Float? = null, preferredY: Float? = null) {
        Resolution.refresh()

        configWindows.find { it.feature == feature }?.let {
            focusWindow(it)
            blurAllWindows(it)
            searchHandler.listening = false
            return
        }

        blurAllWindows()
        searchHandler.listening = false

        val offset = configWindows.size * windowCascadeOffset
        val startX = preferredX ?: (((Resolution.width - defaultWindowWidth) / 2f) + offset)
        val startY = preferredY ?: (((Resolution.height - defaultWindowHeight) / 2f) + offset)

        configWindows.add(
            FeatureConfigWindow(
                feature = feature,
                startX = startX,
                startY = startY,
                startWidth = defaultWindowWidth,
                startHeight = defaultWindowHeight
            ).also { it.clampToScreen() }
        )
    }

    fun isMouseOverConfigWindow(mouseX: Int, mouseY: Int): Boolean {
        return configWindows.asReversed().any { it.contains(mouseX.toFloat(), mouseY.toFloat()) }
    }

    private fun focusWindow(window: FeatureConfigWindow) {
        configWindows.remove(window)
        configWindows.add(window)
    }

    private fun blurAllWindows(except: FeatureConfigWindow? = null) {
        configWindows.forEach { if (it !== except) it.blur() }
    }

    private fun closeWindow(window: FeatureConfigWindow) {
        window.blur()
        configWindows.remove(window)
    }

    private fun closeAllFeatureWindows() {
        blurAllWindows()
        configWindows.clear()
    }

    override fun onClose() {
        closeAllFeatureWindows()
        searchHandler.listening = false
        Config.save()
        super.onClose()
    }

    override fun isPauseScreen(): Boolean = false
}