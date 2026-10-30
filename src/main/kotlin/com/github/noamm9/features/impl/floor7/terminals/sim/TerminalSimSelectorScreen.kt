package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.ConfigManager
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver
import com.github.noamm9.ui.utils.components.UIButton
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component

class TerminalSimSelectorScreen: Screen(Component.literal("Terminal Simulator")) {
    companion object {
        private const val COLUMNS = 2
        private const val BUTTON_WIDTH = 120
        private const val BUTTON_HEIGHT = 20
        private const val BUTTON_GAP = 6
        var clickedRandom = false
    }

    private val buttonRows get() = (SimTerminal.all.size + 2 + COLUMNS - 1) / COLUMNS
    private val startY get() = (height - buttonRows * (BUTTON_HEIGHT + BUTTON_GAP) - font.lineHeight) / 2 + font.lineHeight + BUTTON_GAP
    private val autoNextText get() = "Auto Next: ${if (TerminalSolver.simAutoNext.value) "§aON" else "§cOFF"}"

    override fun init() {
        super.init()

        val startX: Int = (width - COLUMNS * BUTTON_WIDTH - (COLUMNS - 1) * BUTTON_GAP) / 2

        fun addButton(index: Int, text: String, action: (UIButton) -> Unit) {
            val x = startX + index % COLUMNS * (BUTTON_WIDTH + BUTTON_GAP)
            val y = startY + index / COLUMNS * (BUTTON_HEIGHT + BUTTON_GAP)
            addRenderableWidget(UIButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, text, action = action))
        }


        SimTerminal.all.forEachIndexed { index, sim ->
            addButton(index, sim.terminal.displayName) {
                clickedRandom = false
                GuiUtils.setScreen(TerminalSimScreen(sim))
            }
        }
        addButton(SimTerminal.all.size, "Random") {
            clickedRandom = true
            GuiUtils.setScreen(TerminalSimScreen(SimTerminal.all.random()))
        }

        addButton(SimTerminal.all.size + 1, autoNextText) { button ->
            TerminalSolver.simAutoNext.value = ! TerminalSolver.simAutoNext.value
            button.message = Component.literal(autoNextText)
            ConfigManager.save()
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        graphics.drawCenteredString("§lTerminal Simulator", width / 2f, startY - font.lineHeight - BUTTON_GAP)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        return if (KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value == event.key) {
            onClose()
            true
        }
        else super.keyPressed(event)
    }

    override fun isPauseScreen() = false
}