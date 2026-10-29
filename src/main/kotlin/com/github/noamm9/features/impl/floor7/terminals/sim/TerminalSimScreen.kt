package com.github.noamm9.features.impl.floor7.terminals.sim

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.TerminalEvent
import com.github.noamm9.features.impl.floor7.terminals.TerminalClick
import com.github.noamm9.features.impl.floor7.terminals.TerminalSolver
import com.github.noamm9.features.impl.floor7.terminals.impl.MelodyTerminal
import com.github.noamm9.features.impl.floor7.terminals.impl.RubixTerminal
import com.github.noamm9.utils.*
import com.github.noamm9.utils.NumbersUtils.toFixed
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

class TerminalSimScreen(private val sim: SimTerminal): Screen(Component.literal("Terminal Simulator")) {
    private val clickedSlots = mutableListOf<TerminalClick>()
    private var openTime = 0L
    val terminal = sim.terminal

    override fun added() {
        super.added()
        terminal.reset()
        TerminalSolver.resetLayout()
        sim.generate()
        clickedSlots.clear()
        openTime = System.currentTimeMillis()

        if (terminal is MelodyTerminal) sim.items.forEach { (slot, stack) -> terminal.solve(sim.items, sim.title, slot, stack) }
        else terminal.solve(sim.items, sim.title, 0, ItemStack.EMPTY)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) = Unit

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        TerminalSolver.renderTerminal(graphics, terminal, sim.items)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        click()
        return true
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        when (keyEvent.key) {
            KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value -> onClose()
            KeyMappingHelper.getBoundKeyOf(mc.options.keyDrop).value -> click()
            else -> return super.keyPressed(keyEvent)
        }

        return true
    }

    override fun tick() = sync(sim.tick())

    override fun onClose() = GuiUtils.setScreen(TerminalSimSelectorScreen())

    override fun removed() {
        terminal.reset()
        EventBus.post(TerminalEvent.Close)
        super.removed()
    }

    override fun isPauseScreen() = false

    private fun click() {
        val click = with(TerminalSolver) { terminal.hoveredClick() } ?: return
        click(click)
    }

    fun click(click: TerminalClick) {
        if (isFirstClickDelay()) return

        terminal.onSlotClick(click.slotId)
        clickedSlots.add(click)
        TerminalSolver.playClickSound()

        ThreadUtils.scheduledTask(1) {
            if (mc.gui.screen() !== this) return@scheduledTask
            sync(sim.click(click))
        }
    }

    private fun sync(changedSlots: List<Int>) {
        if (terminal !is RubixTerminal) for (slot in changedSlots) {
            terminal.solve(sim.items, sim.title, slot, sim.items[slot] ?: ItemStack.EMPTY)
            if (terminal !is MelodyTerminal) clickedSlots.forEach(terminal::predict)
        }

        if (! (sim.isSolved() || terminal.solution.isEmpty())) return
        val seconds = (System.currentTimeMillis() - openTime) / 1000.0
        ChatUtils.modMessage("§a${terminal.displayName} §7completed in §f${seconds.toFixed(2)}s")
        GuiUtils.setScreen(
            if (TerminalSolver.simAutoNext.value) TerminalSimScreen(SimTerminal.all.random())
            else TerminalSimSelectorScreen()
        )
    }

    fun isFirstClickDelay(): Boolean {
        var delay = TerminalSolver.fcDelay.value
        if (terminal is MelodyTerminal && delay != 0) delay += 3
        return System.currentTimeMillis() - openTime < delay * 50
    }
}