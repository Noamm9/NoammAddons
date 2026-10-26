package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.ui.utils.components.UIButton
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class TerminalSimSelectorScreen: Screen(Component.literal("Terminal Simulator")) {
    private companion object {
        private const val BUTTON_WIDTH = 120
        private const val BUTTON_HEIGHT = 20
        private const val BUTTON_GAP = 4
    }

    private val startY get() = height / 2 - SimTerminal.all.size * (BUTTON_HEIGHT + BUTTON_GAP) / 2

    override fun init() {
        super.init()

        SimTerminal.all.forEachIndexed { index, sim ->
            val y = startY + index * (BUTTON_HEIGHT + BUTTON_GAP)
            addRenderableWidget(UIButton(width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT, sim.terminal.displayName) {
                GuiUtils.setScreen(TerminalSimScreen(sim))
            })
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        graphics.drawCenteredString("§lTerminal Simulator", width / 2f, startY - 16f)
    }

    override fun isPauseScreen() = false
}