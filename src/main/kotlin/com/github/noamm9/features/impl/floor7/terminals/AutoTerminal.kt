package com.github.noamm9.features.impl.floor7.terminals

//#if CHEAT

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.floor7.terminals.TerminalListener.FIRST_CLICK_DELAY
import com.github.noamm9.features.impl.floor7.terminals.impl.*
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.*
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.UKeyboard
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper

object AutoTerminal: Feature("Automatically clicks terminals for you.") {
    private val randomDelay by ToggleSetting("Random Delay", true).withDescription("Normal distributed by min and max").section("Settings").jsonName("randomDelay")
    private val autoDelay by SliderSetting("Click Delay", 150.0, 100.0, 500.0, 1.0).withDescription("Fixed delay between clicks in milliseconds.").hideIf { randomDelay.value }.jsonName("autoDelay")
    private val minRandomDelay by SliderSetting("Min Random Delay", 120.0, 80.0, 500.0, 1.0).withDescription("The minimum possible delay").showIf { randomDelay.value }.jsonName("minRandomDelay")
    private val maxRandomDelay by SliderSetting("Max Random Delay", 150.0, 120.0, 500.0, 1.0).withDescription("The maximum possible delay").showIf { randomDelay.value }.jsonName("maxRandomDelay")
    private val clickOrder by DropdownSetting("Click Order", 2, listOf("None", "Random", "Human", "Skizo")).withDescription("Human: Logic pathing. Skizo: Chaotic/Furthest.")
    private val invwalk by ToggleSetting("Fake InvWalk").withDescription("Draws the Term name and progress on screen rather then the solution")

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

    override fun init() {
        hudElement(
            "AutoTerminal - FakeInvWalk",
            centered = true,
            enabled = { invwalk.value },
            shouldDraw = { TerminalListener.currentHandler?.enabled() == true }
        ) { ctx, e ->
            val handler = if (e) NumberTerminal else TerminalListener.currentHandler !!
            val maxClicks = handler.maxClicks()
            val completed = handler.completedClicks()

            val title = "§3In Terminal (${handler.displayName})"
            val progress = "§b[${completed.coerceIn(0, maxClicks)}/$maxClicks]${handler.progressSuffix()}"

            ctx.drawCenteredString(title, 0, 0)
            if (maxClicks != null && maxClicks > 0) ctx.drawCenteredString(progress, 0, - 10f)

            maxOf(title.width(), progress.width()).toFloat() to 20f
        }.apply {
            x = Resolution.width / 2
            y = Resolution.height / 2 - Resolution.height / 10
            scale = 3f
        }

        register<TickEvent.Server> {
            if (! autoMelody.value) return@register
            if (! TerminalListener.inTerm) return@register
            if (melodyFcDelay.value && TerminalListener.checkFcDelay()) return@register
            val handler = TerminalListener.currentHandler ?: return@register
            if (handler !is MelodyTerminal) return@register
            if (System.currentTimeMillis() - lastClickTime < 250) return@register

            val current = MelodyTerminal.current ?: return@register
            val correct = MelodyTerminal.correct ?: return@register
            val buttonRow = MelodyTerminal.buttonRow ?: return@register
            if (current != correct) return@register

            val actualSlot = buttonRow * 9 + 16
            if (lastClickedSlot == actualSlot) return@register

            clickSlot(actualSlot)
            lastClickTime = System.currentTimeMillis()
            lastClickedSlot = actualSlot

            if (buttonRow == 2) return@register
            if (! melodySkip.value) return@register
            if (! melodySkipFirstRow.value && buttonRow == 0 && current != 4) return@register
            if (! (melodySkipMode.value == 1 || (melodySkipMode.value == 0 && (current == 0 || current == 4)))) return@register

            val windowId = TerminalListener.lastWindowId
            val check = { TerminalListener.inTerm && TerminalListener.currentHandler is MelodyTerminal && windowId == TerminalListener.lastWindowId }
            if (buttonRow < 3) ThreadUtils.scheduledTask(1) { if (check()) clickSlot(actualSlot + 9) }
            if (buttonRow < 2) ThreadUtils.scheduledTask(2) { if (check()) clickSlot(actualSlot + 18) }
        }

        register<TickEvent.Server> {
            val handler = TerminalListener.currentHandler ?: return@register
            if (handler is MelodyTerminal) return@register
            if (! handler.enabled()) return@register
            if (handler.solution.isEmpty()) return@register
            if (TerminalListener.checkFcDelay()) return@register
            if (System.currentTimeMillis() - lastClickTime < getDelay()) return@register
            lastClickTime = System.currentTimeMillis()

            handler.autoClick()
        }

        register<ContainerEvent.MouseClick>(EventPriority.HIGH) {
            val handler = TerminalListener.currentHandler ?: return@register
            if (handler.enabled()) event.isCanceled = true
        }

        register<ContainerEvent.Keyboard>(EventPriority.HIGH) {
            if (event.key.equalsOneOf(KeyMappingHelper.getBoundKeyOf(mc.options.keyInventory).value, UKeyboard.KEY_ESCAPE)) return@register
            val handler = TerminalListener.currentHandler ?: return@register
            if (handler.enabled()) event.isCanceled = true
        }

        register<ScreenEvent.PreRender>(EventPriority.HIGH) {
            if (! invwalk.value) return@register
            val handler = TerminalListener.currentHandler ?: return@register
            if (handler.enabled()) event.isCanceled = true
        }

        register<TerminalEvent.Close> {
            lastClickedSlot = null
            lastClickTime = 0L
        }
    }

    private fun Terminal.autoClick() {
        val rawClick = if (this == NumberTerminal) solution.first()
        else when (clickOrder.value) {
            1 -> solution.random()
            2 -> HumanClickOrder.getBestClick(this)
            3 -> HumanClickOrder.getWorstClick(this)
            else -> solution.first()
        }

        if (lastClickedSlot == rawClick.slotId && this !is RubixTerminal) return

        val finalClick = if (this is RubixTerminal) getClickForSlot(rawClick.slotId) !! else rawClick

        lastClickedSlot = finalClick.slotId
        predict(finalClick)
        finalClick.send()
    }

    private fun Terminal.enabled() = when (this) {
        is NumberTerminal -> autoNumbers.value
        is ColorsTerminal -> autoColors.value
        is MelodyTerminal -> autoMelody.value
        is RubixTerminal -> autoRubix.value
        is RedGreenTerminal -> autoRedGreen.value
        is StartWithTerminal -> autoStartWith.value
    }

    private fun getDelay() = when {
        TerminalListener.checkFcDelay() -> FIRST_CLICK_DELAY * 50
        randomDelay.value -> {
            val min = minRandomDelay.value.toInt().coerceAtLeast(0)
            val max = maxRandomDelay.value.toInt().coerceAtLeast(0)
            if (min == max) min else MathUtils.gaussianRandom(minOf(min, max), maxOf(min, max))
        }

        else -> autoDelay.value.toInt()
    }.coerceAtLeast(0)

    private fun clickSlot(slot: Int) = TerminalClick(slot).send()
}
//#endif