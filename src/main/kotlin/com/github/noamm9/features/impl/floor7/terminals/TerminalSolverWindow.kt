package com.github.noamm9.features.impl.floor7.terminals

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.floor7.terminals.impl.MelodyTerminal
import com.github.noamm9.features.impl.floor7.terminals.impl.RubixTerminal
import com.github.noamm9.features.impl.floor7.terminals.sim.SimTerminal
import com.github.noamm9.ui.clickgui.components.FeatureConfigWindow
import com.github.noamm9.ui.clickgui.components.settings.Style
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawRect
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import java.awt.Color
import kotlin.math.min

/**
 * The Terminal Solver's config window, with a live terminal next to the settings, the way the
 * Dungeon Map's window shows the map. The preview is a small simulator: pick a terminal on the tabs,
 * click its solution to test it, right click for a new one. It is drawn with the solver's own menu
 * code every frame, so any setting changed in the window shows at once.
 */
class TerminalSolverWindow(feature: Feature): FeatureConfigWindow(feature) {
    companion object {
        private const val TAB_COLUMNS = 3
        private const val TAB_HEIGHT = 14f
        private const val TAB_GAP = 2f
        private const val MARGIN = 6f
        private const val FOOTER_HEIGHT = 14f
        private const val TICK_MS = 50L
        private const val NEXT_AFTER_SOLVE_MS = 700L
        private const val MAX_MENU_SCALE = 3f
    }

    override val previewWidth = 240f
    override val defaultHeight = 300f
    override val minHeight = 220f
    override val useSidebar = true

    private val sims get() = SimTerminal.all

    private var sim: SimTerminal? = null
    private var minCol = 0
    private var minRow = 0
    private val clicked = mutableListOf<TerminalClick>()
    private var hoveredSlot: Int? = null
    private var lastTick = 0L
    private var solvedAt = 0L
    private var lastSection: String? = null
    private var lastDropTime = 0L

    // Where the menu's grid was drawn last frame, for mouse hit tests.
    private var menuX = 0f
    private var menuY = 0f
    private var menuScale = 1f

    override fun drawPreview(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float) {
        followSection()
        val sim = sim ?: start(sims.firstOrNull { TerminalSolver.isSolverEnabled(it.terminal) } ?: sims.firstOrNull() ?: return)
        advance(sim)

        drawTabs(context, x, y, width)
        drawMenu(context, sim)
        drawFooter(context, sim, x, y, width, height)
    }

    override fun previewClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        tabAt(mouseX, mouseY)?.let { index ->
            if (button != InputConstants.MOUSE_BUTTON_LEFT) return true
            if (sims[index] !== sim) Style.playClickSound(1f)
            start(sims[index])
            return true
        }

        val sim = sim ?: return false
        when (button) {
            InputConstants.MOUSE_BUTTON_LEFT -> click(sim)
            InputConstants.MOUSE_BUTTON_RIGHT -> start(sim)
            else -> return false
        }
        return true
    }

    /** The drop key clicks the slot under the mouse, at most Max CPS times a second, as in a real terminal. */
    override fun previewKeyPressed(keyCode: Int): Boolean {
        if (keyCode != KeyMappingHelper.getBoundKeyOf(mc.options.keyDrop).value) return false
        val sim = sim ?: return false
        if (hoveredSlot == null) return false

        val now = System.currentTimeMillis()
        if (now - lastDropTime >= 1000 / TerminalSolver.maxCps.value) {
            lastDropTime = now
            click(sim)
        }
        return true
    }

    /** Opening the Rubix, Numbers or Melody category in the sidebar shows that terminal. */
    private fun followSection() {
        val section = selectedSection
        if (section == lastSection) return
        lastSection = section
        sims.find { it.terminal.displayName.equals(section, ignoreCase = true) }?.takeIf { it !== sim }?.let(::start)
    }

    private fun start(next: SimTerminal): SimTerminal {
        sim?.terminal?.reset()
        val terminal = next.terminal
        terminal.reset()
        next.generate()
        clicked.clear()
        hoveredSlot = null
        solvedAt = 0L
        lastTick = System.currentTimeMillis()

        if (terminal is MelodyTerminal) next.items.forEach { (slot, stack) -> terminal.solve(next.items, next.title, slot, stack) }
        else terminal.solve(next.items, next.title, 0, ItemStack.EMPTY)

        // The grid is placed the way the real menu places it: from the first solution's corner.
        val slots = terminal.solution.map { it.slotId }
        minCol = slots.minOfOrNull { it % 9 } ?: 0
        minRow = slots.minOfOrNull { it / 9 } ?: 0

        sim = next
        return next
    }

    /** Runs the simulator's ticks (Melody moves on them) and deals a new terminal after one is solved. */
    private fun advance(sim: SimTerminal) {
        val now = System.currentTimeMillis()
        if (solvedAt != 0L) {
            if (now - solvedAt >= NEXT_AFTER_SOLVE_MS) start(sim)
            return
        }

        if (now - lastTick > 1000L) lastTick = now - TICK_MS
        while (now - lastTick >= TICK_MS && solvedAt == 0L) {
            lastTick += TICK_MS
            sync(sim, sim.tick())
        }
    }

    private fun click(sim: SimTerminal) {
        if (solvedAt != 0L) return
        val terminal = sim.terminal
        val click = hoveredSlot?.let(terminal::getClickForSlot) ?: return

        terminal.predict(click)
        terminal.onSlotClick(click.slotId)
        clicked.add(click)
        TerminalSolver.playClickSound()
        sync(sim, sim.click(click))
    }

    /** What the simulator screen does with the slots a click or a tick changed. */
    private fun sync(sim: SimTerminal, changedSlots: List<Int>) {
        val terminal = sim.terminal
        if (terminal !is RubixTerminal) for (slot in changedSlots) {
            terminal.solve(sim.items, sim.title, slot, sim.items[slot] ?: ItemStack.EMPTY)
            if (terminal !is MelodyTerminal) clicked.forEach(terminal::predict)
        }

        if (sim.isSolved() || (terminal !is MelodyTerminal && terminal.solution.isEmpty())) {
            solvedAt = System.currentTimeMillis()
        }
    }

    private fun drawTabs(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float) {
        sims.forEachIndexed { index, tabSim ->
            val (tabX, tabY, tabW) = tabRect(index, x, y, width)
            val selected = tabSim === sim
            val hovered = lastMouseX in tabX .. tabX + tabW && lastMouseY in tabY .. tabY + TAB_HEIGHT

            val background = when {
                selected -> Style.accentColor.withAlpha(60)
                hovered -> Color(255, 255, 255, 30)
                else -> Color(255, 255, 255, 14)
            }
            context.drawRect(tabX, tabY, tabW, TAB_HEIGHT, background)
            if (selected) context.drawRect(tabX, tabY + TAB_HEIGHT - 1f, tabW, 1f, Style.accentColor)

            val textColor = when {
                ! TerminalSolver.isSolverEnabled(tabSim.terminal) -> Color.DARK_GRAY
                selected -> Style.accentColor
                else -> Color.LIGHT_GRAY
            }
            context.drawCenteredString(tabSim.terminal.displayName, tabX + tabW / 2f, tabY + 3f, textColor)
        }
    }

    private fun drawMenu(context: GuiGraphicsExtractor, sim: SimTerminal) {
        if (! layoutMenu(sim)) return
        val mx = (lastMouseX - menuX) / menuScale
        val my = (lastMouseY - menuY) / menuScale

        context.pose().pushMatrix()
        context.pose().translate(menuX, menuY)
        context.pose().scale(menuScale)
        TerminalSolver.drawTerminal(context, sim.terminal, sim.items, minCol, minRow, mx, my, ::hoveredSlot)
        context.pose().popMatrix()

        // A click outside the menu clicks nothing.
        val (areaX, areaY, areaW, areaH) = menuArea()
        if (lastMouseX < areaX || lastMouseX > areaX + areaW || lastMouseY < areaY || lastMouseY > areaY + areaH) hoveredSlot = null
    }

    /** The part of the preview the menu is fitted into: x, y, width, height. */
    private fun menuArea() = floatArrayOf(
        previewX + MARGIN,
        previewY + tabsHeight() + MARGIN,
        previewWidth - MARGIN * 2,
        previewHeight - tabsHeight() - FOOTER_HEIGHT - MARGIN * 2,
    )

    /** Places the menu's grid in the preview for this frame. False when there is no room for it. */
    private fun layoutMenu(sim: SimTerminal): Boolean {
        val (gridW, gridH) = TerminalSolver.gridSpan(sim.terminal)
        val (left, top, right, bottom) = TerminalSolver.menuMargins()
        val (areaX, areaY, areaW, areaH) = menuArea()
        if (areaW <= 0f || areaH <= 0f) return false

        val totalW = left + gridW + right
        val totalH = top + gridH + bottom
        menuScale = min(MAX_MENU_SCALE, min(areaW / totalW, areaH / totalH))
        menuX = areaX + (areaW - totalW * menuScale) / 2f + left * menuScale
        menuY = areaY + (areaH - totalH * menuScale) / 2f + top * menuScale
        return true
    }

    /**
     * The window's panels are drawn around the terminal's own background (the grid and its padding)
     * and not under it, so the solver's Background Color shows as it does over the game.
     */
    override fun drawBackground(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, color: Color) {
        val sim = sim
        if (sim == null || ! layoutMenu(sim)) return super.drawBackground(context, x, y, width, height, color)

        val (gridW, gridH) = TerminalSolver.gridSpan(sim.terminal)
        val pad = TerminalSolver.menuMargins()[0]
        val holeX = (menuX - pad * menuScale).coerceIn(x, x + width)
        val holeY = (menuY - pad * menuScale).coerceIn(y, y + height)
        val holeRight = (menuX + (gridW + pad) * menuScale).coerceIn(holeX, x + width)
        val holeBottom = (menuY + (gridH + pad) * menuScale).coerceIn(holeY, y + height)

        context.drawRect(x, y, width, holeY - y, color)
        context.drawRect(x, holeBottom, width, y + height - holeBottom, color)
        context.drawRect(x, holeY, holeX - x, holeBottom - holeY, color)
        context.drawRect(holeRight, holeY, x + width - holeRight, holeBottom - holeY, color)
    }

    private fun drawFooter(context: GuiGraphicsExtractor, sim: SimTerminal, x: Float, y: Float, width: Float, height: Float) {
        val text = when {
            solvedAt != 0L -> "§aSolved!"
            ! TerminalSolver.isSolverEnabled(sim.terminal) -> "§cThe solver is off for ${sim.terminal.displayName}"
            else -> "§7Click or drop to test, right click for new"
        }
        context.drawCenteredString(text, x + width / 2f, y + height - FOOTER_HEIGHT + 2f, scale = 0.8f)
    }

    private fun tabsHeight(): Float {
        val rows = (sims.size + TAB_COLUMNS - 1) / TAB_COLUMNS
        return MARGIN + rows * TAB_HEIGHT + (rows - 1).coerceAtLeast(0) * TAB_GAP
    }

    private fun tabRect(index: Int, x: Float, y: Float, width: Float): Triple<Float, Float, Float> {
        val tabW = (width - MARGIN * 2 - TAB_GAP * (TAB_COLUMNS - 1)) / TAB_COLUMNS
        val tabX = x + MARGIN + (index % TAB_COLUMNS) * (tabW + TAB_GAP)
        val tabY = y + MARGIN + (index / TAB_COLUMNS) * (TAB_HEIGHT + TAB_GAP)
        return Triple(tabX, tabY, tabW)
    }

    private fun tabAt(mouseX: Float, mouseY: Float): Int? = sims.indices.firstOrNull { index ->
        val (tabX, tabY, tabW) = tabRect(index, previewX, previewY, previewWidth)
        mouseX in tabX .. tabX + tabW && mouseY in tabY .. tabY + TAB_HEIGHT
    }
}
