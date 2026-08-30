package com.github.noamm9.ui.hud

import com.github.noamm9.config.ConfigManager
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.ui.utils.componnents.UIButton
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color

class HudEditorScreen: Screen(Component.literal("HudEditor")) {
    private val huds = FeatureManager.hudElements.filter { it.toggle }
    private var resetConfirmed = false
    private var gridEnabled = false
    private var gridSize = 10

    private lateinit var gridToggleButton: UIButton

    override fun init() {
        super.init()

        val btnHeight = 20
        val center = width / 2

        gridToggleButton = addRenderableWidget(UIButton(center - 117, height - 40, 80, btnHeight, "§7Grid: OFF") {
            gridEnabled = ! gridEnabled
            updateGridButton()
        })

        addRenderableWidget(UIButton(center - 32, height - 40, 35, btnHeight, "§7-") {
            gridSize = (gridSize - 5).coerceAtLeast(5)
            if (! gridEnabled) gridEnabled = true
            updateGridButton()
        })

        addRenderableWidget(UIButton(center + 8, height - 40, 35, btnHeight, "§7+") {
            gridSize = (gridSize + 5).coerceAtMost(50)
            if (! gridEnabled) gridEnabled = true
            updateGridButton()
        })

        addRenderableWidget(UIButton(center + 48, height - 40, 70, btnHeight, "§cReset") { button ->
            if (! resetConfirmed) {
                button.message = Component.literal("§c§lConfirm?")
                resetConfirmed = true
                return@UIButton
            }
            resetConfirmed = false
            button.message = Component.literal("§cReset")
            FeatureManager.hudElements.forEach { element ->
                element.x = 20f
                element.y = 20f
                element.scale = 1f
            }
        })
    }

    private fun updateGridButton() {
        gridToggleButton.message = Component.literal(
            if (gridEnabled) "§aGrid: ON §7(${gridSize}px)" else "§7Grid: OFF"
        )
    }

    private fun drawGrid(ctx: GuiGraphicsExtractor) {
        val gridColor = Color(255, 255, 255, 30).rgb
        val w = Resolution.width.toInt()
        val h = Resolution.height.toInt()

        var x = 0
        while (x <= w) {
            ctx.fill(x, 0, x + 1, h, gridColor)
            x += gridSize
        }

        var y = 0
        while (y <= h) {
            ctx.fill(0, y, w, y + 1, gridColor)
            y += gridSize
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        Resolution.push(graphics)

        val mX = Resolution.getMouseX(mouseX)
        val mY = Resolution.getMouseY(mouseY)
        val midX = Resolution.width / 2

        if (gridEnabled) drawGrid(graphics)

        val effectiveGridSize = if (gridEnabled) gridSize else 0
        for (hud in huds) hud.drawEditor(graphics, mX, mY, effectiveGridSize)

        val draggedElement = huds.find { it.isDragging }
        val hoveredElement = if (draggedElement == null) huds.find { it.isHovered(mX, mY) } else null
        val activeElement = draggedElement ?: hoveredElement

        if (activeElement != null) {
            val coords = "§7(${activeElement.x.toInt()}, ${activeElement.y.toInt()}) §7@ §f${"%.1f".format(activeElement.scale)}x"
            graphics.drawCenteredString("${activeElement.name} $coords", midX, 10f, scale = 1.2f)
        }

        Resolution.pop(graphics)
        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        for (hud in huds) if (hud.isDragging) {
            val increment = (vertical * 0.1).toFloat()
            hud.scale = (hud.scale + increment).coerceIn(0.5f, 5.0f)
            return true
        }

        if (gridEnabled) {
            val step = (vertical * 5).toInt()
            if (step != 0) {
                gridSize = (gridSize + step).coerceIn(5, 50)
                return true
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (super.mouseClicked(mouseButtonEvent, bl)) return true

        val mX = Resolution.getMouseX(mouseButtonEvent.x)
        val mY = Resolution.getMouseY(mouseButtonEvent.y)

        if (mouseButtonEvent.button() == 0) huds.forEach {
            it.startDragging(mX, mY)
            if (it.isDragging) return true
        }

        return false
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        for (hud in huds) hud.isDragging = false
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun onClose() {
        ConfigManager.save()
        super.onClose()
    }
}