package com.github.noamm9.features.impl.floor7.terminals

//#if CHEAT

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.floor7.terminals.TerminalListener.FIRST_CLICK_DELAY
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.ThreadUtils
import net.minecraft.world.inventory.ClickType

object AutoTerminal: Feature("Automatically clicks terminals for you.") {
    private val randomDelay by ToggleSetting("Random Delay", true).section("Settings")

    private val autoDelay by SliderSetting("Click Delay", 150.0, 0.0, 500.0, 10.0)
        .withDescription("Fixed delay between clicks in milliseconds.")
        .showIf { ! randomDelay.value }

    private val minRandomDelay by SliderSetting("Min Random Delay", 50.0, 0.0, 500.0, 10.0)
        .withDescription("The minimum possible delay")
        .showIf { randomDelay.value }

    private val maxRandomDelay by SliderSetting("Max Random Delay", 150.0, 0.0, 500.0, 10.0)
        .withDescription("The maximum possible delay")
        .showIf { randomDelay.value }

    private val clickOrder by DropdownSetting("Click Order", 2, listOf("None", "Random", "Human", "Skizo"))
        .withDescription("Human: Logic pathing. Skizo: Chaotic/Furthest.")

    private val autoMelody by ToggleSetting("Melody", true).section("Melody-AutoTerm")
    private val melodyFcDelay by ToggleSetting("First Click Delay", true).showIf { autoMelody.value }
    private val melodySkip by ToggleSetting("Melody Skip").showIf { autoMelody.value }
    private val melodySkipMode by DropdownSetting("Skip Mode", 0, listOf("Edges", "All")).showIf { autoMelody.value && melodySkip.value }
    private val melodySkipFirstRow by ToggleSetting("&cSkip First Row").showIf { autoMelody.value && melodySkip.value }

    private val autoNumbers by ToggleSetting("Numbers", true).section("Terminals")
    private val autoColors by ToggleSetting("Colors", true)
    private val autoRubix by ToggleSetting("Rubix", true)
    private val autoRedGreen by ToggleSetting("Red-Green", true)
    private val autoStartWith by ToggleSetting("Start-With", true)

    private var lastClickTime = 0L
    private var lastClickedSlot: Int? = null


    override fun onEnable() {
        super.onEnable()
        TerminalListener.packetRecivedListener.register()
        TerminalListener.packetSentListener.register()
        TerminalListener.tickListener.register()
        TerminalListener.worldChangeListener.register()
        Scheduler.tickListener.register()
        Scheduler.timeListener.register()
    }

    override fun onDisable() {
        super.onDisable()
        if (TerminalSolver.enabled) return
        TerminalListener.packetRecivedListener.unregister()
        TerminalListener.packetSentListener.unregister()
        TerminalListener.tickListener.unregister()
        TerminalListener.worldChangeListener.unregister()
        Scheduler.timeListener.unregister()
        Scheduler.tickListener.unregister()
    }

    override fun init() {
        register<TickEvent.Server> {
            if (! autoMelody.value) return@register
            if (! TerminalListener.inTerm) return@register
            if (melodyFcDelay.value && TerminalListener.checkFcDelay()) return@register
            if (TerminalListener.currentType != TerminalType.MELODY) return@register
            if (System.currentTimeMillis() - lastClickTime < 250) return@register

            val current = TerminalType.melodyCurrent ?: return@register
            val correct = TerminalType.melodyCorrect ?: return@register
            val buttonRow = TerminalType.melodyButton ?: return@register
            if (current != correct) return@register

            val actualSlot = buttonRow * 9 + 16
            if (lastClickedSlot == actualSlot) return@register

            ThreadUtils.scheduledTask(0) { sendClickPacket(actualSlot) }
            lastClickTime = System.currentTimeMillis()
            lastClickedSlot = actualSlot


            if (buttonRow == 3) return@register
            if (! melodySkip.value) return@register
            if (! melodySkipFirstRow.value && buttonRow == 0) return@register
            if (! (melodySkipMode.value == 1 || (melodySkipMode.value == 0 && (current == 0 || current == 4)))) return@register

            val check = { TerminalListener.inTerm && TerminalListener.currentType == TerminalType.MELODY }
            if (buttonRow < 3) ThreadUtils.scheduledTask(1) { if (check()) sendClickPacket(actualSlot + 9) }
            if (buttonRow < 2) ThreadUtils.scheduledTask(2) { if (check()) sendClickPacket(actualSlot + 18) }
            if (buttonRow < 1) ThreadUtils.scheduledTask(3) { if (check()) sendClickPacket(actualSlot + 27) }
        }
    }

    fun onItemsUpdated() {
        if (! enabled) return
        val type = TerminalListener.currentType ?: return
        if (! shouldAutoSolve(type)) return

        if (TerminalSolver.solution.isEmpty()) {
            TerminalSolver.solve()
        }

        val solution = TerminalSolver.solution
        if (solution.isEmpty()) return

        autoClick(solution, type)
    }

    private fun autoClick(solution: List<TerminalClick>, type: TerminalType) {
        val rawClick = if (type == TerminalType.NUMBERS) solution.first()
        else when (clickOrder.value) {
            1 -> solution.random()
            2 -> HumanClickOrder.getBestClick(solution, type)
            3 -> HumanClickOrder.getWorstClick(solution, type)
            else -> solution.first()
        }

        if (lastClickedSlot == rawClick.slotId && type != TerminalType.RUBIX) return

        val finalClick = if (type == TerminalType.RUBIX)
            TerminalClick(rawClick.slotId, if (rawClick.btn > 0) 0 else 1)
        else rawClick

        val delayMs = when {
            TerminalListener.checkFcDelay() -> FIRST_CLICK_DELAY * 50
            randomDelay.value -> {
                val min = minRandomDelay.value.toInt().coerceAtLeast(0)
                val max = maxRandomDelay.value.toInt().coerceAtLeast(0)
                if (min == max) min else MathUtils.gaussianRandom(minOf(min, max), maxOf(min, max))
            }

            else -> autoDelay.value.toInt()
        }.coerceAtLeast(0)

        val delayTicks = delayMs / 50
        val initialWindowId = TerminalListener.lastWindowId

        if (delayMs == 0) click(finalClick)
        else Scheduler.schedule(delayMs, delayTicks) {
            if (TerminalListener.inTerm && initialWindowId == TerminalListener.lastWindowId) {
                click(finalClick)
            }
        }
    }

    fun shouldAutoSolve(type: TerminalType) = when (type) {
        TerminalType.NUMBERS -> autoNumbers.value
        TerminalType.COLORS -> autoColors.value
        TerminalType.MELODY -> autoMelody.value
        TerminalType.RUBIX -> autoRubix.value
        TerminalType.REDGREEN -> autoRedGreen.value
        TerminalType.STARTWITH -> autoStartWith.value
    }

    private fun click(click: TerminalClick) {
        lastClickedSlot = click.slotId
        TerminalSolver.click(click)
    }

    private fun sendClickPacket(slot: Int) {
        mc.gameMode?.handleInventoryMouseClick(
            TerminalListener.lastWindowId, slot, 2, ClickType.CLONE, mc.player
        )
    }

    fun reset() {
        lastClickTime = 0
        lastClickedSlot = null
    }
}
//#endif