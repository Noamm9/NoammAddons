package com.github.noamm9.ui.clickgui

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.config.ConfigManager
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.Style
import com.github.noamm9.ui.clickgui.enums.CategoryType
import com.github.noamm9.ui.clickgui.enums.WindowClickAction
import com.github.noamm9.ui.hud.HudEditorScreen
import com.github.noamm9.ui.utils.MouseHelper
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.TextInputHandler
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawLine
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawTexture
import com.github.noamm9.utils.render.Render2D.drawVerticalGradient
import gg.essential.universal.UKeyboard
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.awt.Color

class ClickGuiScreen: Screen(Component.literal("ClickGUI")) {
    companion object {
        var INSTANCE: ClickGuiScreen? = null

        private const val defaultWindowWidth = 220f
        private const val defaultWindowHeight = 260f
        private const val windowCascadeOffset = 18f
        private const val searchbarButtonSize = 22f
    }

    private val discordTexture = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/discord.png")

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
            if (value == null) configWindows.clear()
            else openFeatureWindow(value)
        }

    init {
        INSTANCE = this
        CategoryType.entries.forEachIndexed { index, category ->
            panels.add(Panel(category, 20 + (index * 120), 20, this))
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val mX = Resolution.getMouseX(mouseX.toDouble())
        val mY = Resolution.getMouseY(mouseY.toDouble())
        Resolution.push(context)
        TooltipManager.reset()

        context.drawVerticalGradient(0, 0, Resolution.width, Resolution.height, Color(0, 0, 0, 100), Color(0, 0, 0, 150))
        context.drawSearchBar(mX.toFloat(), mY.toFloat())
        context.drawHudButton(mX.toFloat(), mY.toFloat())
        context.drawDiscordButton(mX.toFloat(), mY.toFloat())

        val draggingPanel = panels.find { it.dragging }
        panels.forEach { if (it != draggingPanel) it.render(context, mX, mY) }
        draggingPanel?.render(context, mX, mY)

        configWindows.forEachIndexed { index, window -> window.render(context, mX, mY, index == configWindows.lastIndex) }

        val cursor = configWindows.asReversed().firstNotNullOfOrNull { it.cursorAt(mX.toFloat(), mY.toFloat()) }
        if (cursor == null) MouseHelper.resetCursor()
        else MouseHelper.setCursor(cursor)

        TooltipManager.draw(context, Resolution.width, Resolution.height)
        Resolution.pop(context)
    }

    private fun GuiGraphicsExtractor.drawSearchBar(mX: Float, mY: Float) {
        val bw = 150f
        val bh = 22f
        val bx = (Resolution.width / 2) - (bw / 2)
        val by = Resolution.height - 40

        drawRect(bx, by, bw, bh, Color(15, 15, 15, 200))

        val borderColor = if (searchHandler.listening) Style.accentColor else Color(255, 255, 255, 30)
        drawRect(bx, by + bh - 2, bw, 2f, borderColor)

        searchHandler.x = bx
        searchHandler.y = by
        searchHandler.width = bw
        searchHandler.height = bh

        if (searchQuery.isEmpty() && ! searchHandler.listening) {
            drawCenteredString("§8Search...", Resolution.width / 2, by + 7, Color.GRAY, shadow = false)
        }
        else searchHandler.draw(this, mX, mY)
    }

    private fun GuiGraphicsExtractor.drawHudButton(mX: Float, mY: Float) {
        val x = searchHandler.x + searchHandler.width + 6f
        val y = searchHandler.y + (searchHandler.height - searchbarButtonSize) / 2f

        val hovered = isOverHudButton(mX, mY)
        val borderColor = if (hovered) Style.accentColor else Color(255, 255, 255, 30)

        drawRect(x, y, searchbarButtonSize, searchbarButtonSize, Color(15, 15, 15, 200))
        drawRect(x, y + searchbarButtonSize - 2f, searchbarButtonSize, 2f, borderColor)

        val cx = x + searchbarButtonSize / 2f
        val cy = y + searchbarButtonSize / 2f
        val s = 8.485281f

        val diamondColor = if (hovered) Style.accentColor.withAlpha(40) else Color(255, 255, 255, 12)
        val iconColor = if (hovered) Style.accentColor else Color.WHITE

        pose().pushMatrix()
        pose().translate(cx, cy)
        pose().rotate(45f)
        pose().scale(s, s)
        pose().translate(- 0.5f, - 0.5f)
        fill(0, 0, 1, 1, diamondColor.rgb)
        pose().popMatrix()

        drawLine(cx, cy - 3f, cx, cy + 3f, iconColor)
        drawLine(cx - 3f, cy, cx + 3f, cy, iconColor)
        drawLine(cx - 2f, cy - 3f, cx, cy - 5f, iconColor)
        drawLine(cx + 2f, cy - 3f, cx, cy - 5f, iconColor)
        drawLine(cx - 2f, cy + 3f, cx, cy + 5f, iconColor)
        drawLine(cx + 2f, cy + 3f, cx, cy + 5f, iconColor)
        drawLine(cx - 3f, cy - 2f, cx - 5f, cy, iconColor)
        drawLine(cx - 3f, cy + 2f, cx - 5f, cy, iconColor)
        drawLine(cx + 3f, cy - 2f, cx + 5f, cy, iconColor)
        drawLine(cx + 3f, cy + 2f, cx + 5f, cy, iconColor)
    }

    private fun GuiGraphicsExtractor.drawDiscordButton(mX: Float, mY: Float) {
        val x = searchHandler.x - 6f - searchbarButtonSize
        val y = searchHandler.y + (searchHandler.height - searchbarButtonSize) / 2f

        val hovered = isOverDiscordButton(mX, mY)
        val borderColor = if (hovered) Style.accentColor else Color(255, 255, 255, 30)

        drawRect(x, y, searchbarButtonSize, searchbarButtonSize, Color(15, 15, 15, 200))
        drawRect(x, y + searchbarButtonSize - 2f, searchbarButtonSize, 2f, borderColor)

        val iconSize = 18f
        val ix = x + (searchbarButtonSize - iconSize) / 2f
        val iy = y + (searchbarButtonSize - iconSize) / 2f
        drawTexture(discordTexture, ix, iy, iconSize, iconSize)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = Resolution.getMouseX(mouseButtonEvent.x)
        val my = Resolution.getMouseY(mouseButtonEvent.y)
        val button = mouseButtonEvent.button()

        configWindows.asReversed().find { it.contains(mx.toFloat(), my.toFloat()) }?.let { window ->
            focusWindow(window)
            searchHandler.listening = false

            when (window.mouseClicked(mx, my, button)) {
                WindowClickAction.CLOSE -> configWindows.remove(window)
                WindowClickAction.CONSUMED -> {}
            }
            return true
        }

        panels.find { it.isMouseOverHeader(mx.toDouble(), my.toDouble()) }?.let { clickedPanel ->
            clickedPanel.mouseClicked(mx.toDouble(), my.toDouble(), button)
            searchHandler.listening = false
            return true
        }

        if (isOverHudButton(mx.toFloat(), my.toFloat())) {
            if (button == 0) {
                onClose()
                GuiUtils.setScreen(HudEditorScreen())
            }
            return true
        }

        if (isOverDiscordButton(mx.toFloat(), my.toFloat())) {
            if (button == 0) Utils.openDiscordLink()
            return true
        }

        if (searchHandler.mouseClicked(mx.toFloat(), my.toFloat(), mouseButtonEvent)) return true

        panels.asReversed().find { it.isMouseOver(mx, my) }?.let { panel ->
            panel.mouseClicked(mx.toDouble(), my.toDouble(), button)
            return true
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
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
            if (window.charTyped(characterEvent.codepoint.toChar())) {
                return true
            }
        }

        if (searchHandler.keyTyped(characterEvent)) return true
        return super.charTyped(characterEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        configWindows.lastOrNull()?.let { window ->
            if (window.keyPressed(keyEvent.key, keyEvent.scancode, keyEvent.modifiers)) return true
            if (keyEvent.key == UKeyboard.KEY_ESCAPE) {
                configWindows.remove(window)
                return true
            }
        }

        if (searchHandler.keyPressed(keyEvent)) return true
        if (keyEvent.hasControlDown() && keyEvent.input() == UKeyboard.KEY_F) {
            searchHandler.listening = ! searchHandler.listening
            return true
        }

        return super.keyPressed(keyEvent)
    }

    fun openFeatureWindow(feature: Feature, preferredX: Float? = null, preferredY: Float? = null) {
        configWindows.find { it.feature == feature }?.let {
            focusWindow(it)
            searchHandler.listening = false
            return
        }

        searchHandler.listening = false

        val offset = configWindows.size * windowCascadeOffset
        val startX = preferredX ?: (((Resolution.width - defaultWindowWidth) / 2f) + offset)
        val startY = preferredY ?: (((Resolution.height - defaultWindowHeight) / 2f) + offset)

        configWindows.add(FeatureConfigWindow(
            feature = feature,
            startX = startX,
            startY = startY,
            startWidth = defaultWindowWidth,
            startHeight = defaultWindowHeight
        ).also { it.clampToScreen() })
    }

    fun isMouseOverConfigWindow(mouseX: Int, mouseY: Int) = configWindows.asReversed().any { it.contains(mouseX.toFloat(), mouseY.toFloat()) }

    private fun isOverHudButton(mX: Float, mY: Float): Boolean {
        val x = searchHandler.x + searchHandler.width + 6f
        val y = searchHandler.y + (searchHandler.height - searchbarButtonSize) / 2f
        return mX in x .. (x + searchbarButtonSize) && mY in y .. (y + searchbarButtonSize)
    }

    private fun isOverDiscordButton(mX: Float, mY: Float): Boolean {
        val x = searchHandler.x - 6f - searchbarButtonSize
        val y = searchHandler.y + (searchHandler.height - searchbarButtonSize) / 2f
        return mX in x .. (x + searchbarButtonSize) && mY in y .. (y + searchbarButtonSize)
    }

    private fun focusWindow(window: FeatureConfigWindow) {
        configWindows.remove(window)
        configWindows.add(window)
    }

    override fun onClose() {
        INSTANCE = null
        configWindows.clear()
        searchHandler.listening = false
        MouseHelper.resetCursor()
        ConfigManager.save()
        super.onClose()
    }
}