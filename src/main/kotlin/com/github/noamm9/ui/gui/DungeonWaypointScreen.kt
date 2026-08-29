package com.github.noamm9.ui.gui

import com.github.noamm9.features.impl.dungeon.DungeonWaypoints
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.ui.utils.Animation
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.awt.Color

class DungeonWaypointScreen(
    private val roomName: String,
    private val absolutePos: BlockPos,
    private val relativePos: BlockPos,
    private val initialState: DungeonWaypoints.DungeonWaypoint? = null
): Screen(Component.literal("Waypoint Editor")) {
    private companion object {
        private const val PANEL_WIDTH = 380f
        private const val PANEL_HEIGHT = 280f
        private const val TOGGLE_WIDTH = 340f
        private const val TOGGLE_HEIGHT = 22f
        private const val TOGGLES_START = 80f
        private const val TOGGLE_SPACING = 28f
        private const val SWATCH_SIZE = 24f
        private const val SWATCH_GAP = 8f
        private const val SWATCHES_START = 186f
        private const val BUTTON_WIDTH = 150f
        private const val BUTTON_HEIGHT = 24f
        private const val BUTTONS_START = 226f
        private val colorPalette = listOf(
            Color.GREEN, Color.RED, Color.BLUE, Color.CYAN,
            Color.MAGENTA, Color.YELLOW, Color.WHITE, Color.BLACK, Color.ORANGE
        )
        private val swatchGridWidth = SWATCH_SIZE * colorPalette.size + SWATCH_GAP * (colorPalette.size - 1)
    }

    private var filled = true
    private var outline = true
    private var phase = true
    private var selectedColorIndex = 0

    private val toggles = listOf(
        Toggle("Filled", { filled }, { filled = it }),
        Toggle("Outline", { outline }, { outline = it }),
        Toggle("Phase (See-Thru)", { phase }, { phase = it })
    )

    private val colorSwatches = colorPalette.map(::ColorSwatch)
    private val saveButton = ActionButton("§aSave", Color.GREEN)
    private val cancelButton = ActionButton("§cCancel", Color.RED)

    private val panelX get() = (Resolution.width - PANEL_WIDTH) / 2
    private val panelY get() = (Resolution.height - PANEL_HEIGHT) / 2

    override fun init() {
        if (initialState == null) return
        filled = initialState.filled
        outline = initialState.outline
        phase = initialState.phase
        val initialRgb = initialState.color.rgb and 0xFFFFFF
        selectedColorIndex = colorPalette.indexOfFirst { (it.rgb and 0xFFFFFF) == initialRgb }.coerceAtLeast(0)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.push(graphics)

        val mX = Resolution.getMouseX(mouseX).toFloat()
        val mY = Resolution.getMouseY(mouseY).toFloat()
        val x = panelX
        val y = panelY

        graphics.fillGradient(0, 0, Resolution.width.toInt(), Resolution.height.toInt(), Color(0, 0, 0, 100).rgb, Color(0, 0, 0, 150).rgb)

        graphics.drawRect(x, y, PANEL_WIDTH, PANEL_HEIGHT, Color(20, 20, 20, 240))
        graphics.drawRect(x, y, PANEL_WIDTH, 2f, Style.accentColor)
        graphics.drawRect(x, y + PANEL_HEIGHT - 1, PANEL_WIDTH, 1f, Color(255, 255, 255, 18))

        graphics.drawCenteredString("§l${if (initialState == null) "New" else "Edit"} Waypoint", x + PANEL_WIDTH / 2, y + 10)
        graphics.drawCenteredString("§b$roomName", x + PANEL_WIDTH / 2, y + 42)
        graphics.drawCenteredString("§7[${absolutePos.x}, ${absolutePos.y}, ${absolutePos.z}]", x + PANEL_WIDTH / 2, y + 56)

        toggles.forEachIndexed { index, toggle ->
            drawToggle(graphics, toggle, x + 20, y + TOGGLES_START + index * TOGGLE_SPACING, mX, mY)
        }

        graphics.drawString("§7Color", x + 20, y + 168)
        drawPreview(graphics, x + PANEL_WIDTH - 20 - SWATCH_SIZE, y + 164)

        colorSwatches.forEachIndexed { index, swatch ->
            drawColorSwatch(graphics, swatch, index, x, y + SWATCHES_START, mX, mY)
        }

        val noStyleSelected = ! filled && ! outline
        if (noStyleSelected) graphics.drawCenteredString("§cBoth Filled and Outline cannot be false", x + PANEL_WIDTH / 2, y + 258)

        drawActionButton(graphics, saveButton, x + 30, y + BUTTONS_START, mX, mY, enabled = ! noStyleSelected)
        drawActionButton(graphics, cancelButton, x + PANEL_WIDTH - 30 - BUTTON_WIDTH, y + BUTTONS_START, mX, mY)

        Resolution.pop(graphics)
    }

    private fun drawToggle(graphics: GuiGraphicsExtractor, toggle: Toggle, x: Float, y: Float, mx: Float, my: Float) {
        val isOn = toggle.isEnabled()
        val hovered = mx >= x && mx <= x + TOGGLE_WIDTH && my >= y && my <= y + TOGGLE_HEIGHT
        toggle.hoverAnim.update(if (hovered) 1f else 0f)
        toggle.switchAnim.update(if (isOn) 1f else 0f)

        val hover = toggle.hoverAnim.value
        if (isOn) graphics.drawRect(x, y, 2f, TOGGLE_HEIGHT, Style.accentColor.withAlpha((140 + 115 * hover).toInt()))
        graphics.drawRect(x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, Color(255, 255, 255, (10 * hover).toInt()))
        graphics.drawString(toggle.label, x + 12, y + (TOGGLE_HEIGHT - 8) / 2, if (isOn) Color.WHITE else Color.GRAY)

        val switchX = x + TOGGLE_WIDTH - 34
        val switchY = y + (TOGGLE_HEIGHT - 14) / 2
        val anim = toggle.switchAnim.value
        graphics.drawRect(switchX, switchY, 34f, 14f, MathUtils.lerpColor(Color(45, 45, 45, 200), Style.accentColor.withAlpha(200), anim))
        graphics.drawRect(switchX + 2 + anim * 20, switchY + 2, 10f, 10f, Color.WHITE)
    }

    private fun drawPreview(graphics: GuiGraphicsExtractor, x: Float, y: Float) {
        val baseColor = colorPalette[selectedColorIndex]
        graphics.drawRect(x, y, SWATCH_SIZE, SWATCH_SIZE, if (filled) baseColor.withAlpha(60) else Color(0, 0, 0, 0))
        if (outline) graphics.drawBorder(x, y, SWATCH_SIZE, SWATCH_SIZE, baseColor, 2f)
    }

    private fun drawColorSwatch(graphics: GuiGraphicsExtractor, swatch: ColorSwatch, index: Int, panelX: Float, y: Float, mx: Float, my: Float) {
        val x = panelX + (PANEL_WIDTH - swatchGridWidth) / 2 + index * (SWATCH_SIZE + SWATCH_GAP)
        val selected = selectedColorIndex == index
        val hovered = mx >= x && mx <= x + SWATCH_SIZE && my >= y && my <= y + SWATCH_SIZE
        swatch.hoverAnim.update(if (hovered) 1f else 0f)

        graphics.drawRect(x, y, SWATCH_SIZE, SWATCH_SIZE, if (selected) swatch.color.brighter() else swatch.color)
        if (hovered && ! selected) graphics.drawRect(x, y, SWATCH_SIZE, SWATCH_SIZE, Color(255, 255, 255, 18))

        val border = when {
            selected -> Style.accentColor
            hovered -> Color(255, 255, 255, 100)
            else -> Color(0, 0, 0, 150)
        }
        graphics.drawBorder(x, y, SWATCH_SIZE, SWATCH_SIZE, border, if (selected) 2f else 1f)

        if (selected) graphics.drawCenteredString("§f✓", x + SWATCH_SIZE / 2, y + (SWATCH_SIZE - 8) / 2)
    }

    private fun drawActionButton(graphics: GuiGraphicsExtractor, button: ActionButton, x: Float, y: Float, mx: Float, my: Float, enabled: Boolean = true) {
        val hovered = enabled && mx >= x && mx <= x + BUTTON_WIDTH && my >= y && my <= y + BUTTON_HEIGHT
        button.hoverAnim.update(if (hovered) 1f else 0f)

        if (! enabled) {
            graphics.drawRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Color(15, 15, 15, 160))
            graphics.drawRect(x, y + BUTTON_HEIGHT - 1, BUTTON_WIDTH, 2f, Color(255, 255, 255, 25))
            graphics.drawCenteredString(button.label, x + BUTTON_WIDTH / 2, y + (BUTTON_HEIGHT - 8) / 2, Color.GRAY)
            return
        }

        val hover = button.hoverAnim.value
        val bg = (35 + 25 * hover).toInt()
        graphics.drawRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Color(bg, bg, bg, 220))
        graphics.drawRect(x, y + BUTTON_HEIGHT - 1, BUTTON_WIDTH, 2f, button.color.withAlpha((170 + 85 * hover).toInt()))
        graphics.drawCenteredString(button.label, x + BUTTON_WIDTH / 2, y + (BUTTON_HEIGHT - 8) / 2, if (hovered) button.color else Color.WHITE)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, isDoubleClick)

        val mx = Resolution.getMouseX(event.x).toFloat()
        val my = Resolution.getMouseY(event.y).toFloat()
        val x = panelX
        val y = panelY

        if (mx < x || mx > x + PANEL_WIDTH || my < y || my > y + PANEL_HEIGHT) return super.mouseClicked(event, isDoubleClick)

        toggles.forEachIndexed { index, toggle ->
            val toggleY = y + TOGGLES_START + index * TOGGLE_SPACING
            if (mx >= x + 20 && mx <= x + 20 + TOGGLE_WIDTH && my >= toggleY && my <= toggleY + TOGGLE_HEIGHT) {
                toggle.setEnabled(! toggle.isEnabled())
                Style.playClickSound(if (toggle.isEnabled()) 1.2f else 0.9f)
                return true
            }
        }

        colorSwatches.forEachIndexed { index, _ ->
            val swatchX = x + (PANEL_WIDTH - swatchGridWidth) / 2 + index * (SWATCH_SIZE + SWATCH_GAP)
            if (mx >= swatchX && mx <= swatchX + SWATCH_SIZE && my >= y + SWATCHES_START && my <= y + SWATCHES_START + SWATCH_SIZE) {
                selectedColorIndex = index
                Style.playClickSound(1.1f)
                return true
            }
        }

        if (mx >= x + 30 && mx <= x + 30 + BUTTON_WIDTH && my >= y + BUTTONS_START && my <= y + BUTTONS_START + BUTTON_HEIGHT) {
            if (filled || outline) save()
            else {
                ChatUtils.modMessage("§cBoth Filled and Outline cannot be false")
                Style.playClickSound(0.7f)
            }
            return true
        }

        val cancelX = x + PANEL_WIDTH - 30 - BUTTON_WIDTH
        if (mx >= cancelX && mx <= cancelX + BUTTON_WIDTH && my >= y + BUTTONS_START && my <= y + BUTTONS_START + BUTTON_HEIGHT) {
            onClose()
            return true
        }

        return super.mouseClicked(event, isDoubleClick)
    }

    private fun save() {
        val baseColor = colorPalette[selectedColorIndex]
        val finalColor = if (filled) baseColor.withAlpha(60) else baseColor

        DungeonWaypoints.saveWaypoint(absolutePos, relativePos, roomName, finalColor, filled, outline, phase)
        onClose()
    }

    private class Toggle(val label: String, val isEnabled: () -> Boolean, val setEnabled: (Boolean) -> Unit) {
        val hoverAnim = Animation(150L)
        val switchAnim = Animation(150L)
    }

    private class ColorSwatch(val color: Color) {
        val hoverAnim = Animation(150L)
    }

    private class ActionButton(val label: String, val color: Color) {
        val hoverAnim = Animation(150L)
    }
}