package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.width

object TickTimers: Feature("Shows various types of server tick timers for F7 boss fight.") {
    private val showPrefix by ToggleSetting("Prefix", true).section("Settings")
    private val showSuffix by ToggleSetting("Suffix", true)
    private val format by DropdownSetting("Format", 0, listOf("Seconds", "Ticks"))

    private val maxor by ToggleSetting("Maxor Start").section("F7")
    private val goldorDeathTickTimer by ToggleSetting("Goldor Death Ticks")
    private val padTimer by ToggleSetting("Storm Pad Timer")
    private val pyTimer by ToggleSetting("Storm PY Timer")

    private var startTickTime = - 1
    private var goldorTickTime = - 1
    private var padTickTime = - 1
    private var pyTickTime = - 1
    private var stormActive = false
    private var pyTriggered = false

    override fun init() {
        hudElement("Tick Timers", shouldDraw = { LocationUtils.inDungeon }, centered = true) { ctx, example ->
            val textToRender = if (example) "§aStart: 150"
            else when {
                startTickTime != - 1 -> formatTimer(startTickTime, 83, "§aStart:")
                goldorTickTime != - 1 -> formatTimer(goldorTickTime, 60, "§7Goldor:")
                pyTickTime != - 1 -> formatTimer(pyTickTime, 75, "§5PY:")
                padTickTime != - 1 -> formatTimer(padTickTime, 20, "§bPad:")
                else -> return@hudElement 0f to 0f
            }

            ctx.drawCenteredString(textToRender, 0f, 0f)
            return@hudElement textToRender.width() to 9f
        }

        register<WorldChangeEvent> { reset() }

        register<ChatMessageEvent> {
            when (event.unformattedText) {
                "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" -> if (maxor.value) startTickTime = 83

                "[BOSS] Storm: ENERGY HEED MY CALL!", "[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!" -> {
                    if (pyTimer.value && ! pyTriggered) {
                        pyTriggered = true
                        pyTickTime = 62
                    }
                }

                "[BOSS] Storm: I should have known that I stood no chance." -> {
                    if (pyTriggered) {
                        pyTriggered = false
                        pyTickTime = - 1
                    }
                    if (stormActive) {
                        stormActive = false
                        padTickTime = - 1
                    }
                }

                "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                    if (goldorDeathTickTimer.value) goldorTickTime = 60
                }

                "The Core entrance is opening!" -> goldorTickTime = - 1

                "[BOSS] Storm: Pathetic Maxor, just like expected." -> {
                    if (padTimer.value) {
                        padTickTime = 20
                        stormActive = true
                    }
                }
            }
        }

        register<TickEvent.Server> {
            if (startTickTime != - 1) startTickTime --

            if (stormActive && padTickTime != - 1) {
                padTickTime --
                if (padTickTime <= 0) padTickTime = 20
            }

            if (pyTimer.value && pyTickTime >= 0) pyTickTime --

            if (goldorTickTime >= 0) {
                goldorTickTime --
                if (goldorTickTime == 0) goldorTickTime = 60
            }
        }
    }

    private fun reset() {
        padTickTime = - 1
        goldorTickTime = - 1
        startTickTime = - 1
        stormActive = false
        pyTickTime = - 1
        pyTriggered = false
    }

    private fun formatTimer(time: Int, max: Int, prefixText: String): String {
        val color = when {
            time >= max * 0.66 -> "§a"
            time >= max * 0.33 -> "§6"
            else -> "§c"
        }

        val timeDisplay = if (format.value == 1) time.toString()
        else (time / 20f).toFixed(2)

        val prefix = if (showPrefix.value) "$prefixText " else ""
        val suffix = if (showSuffix.value) if (format.value == 1) "t" else "s" else ""

        return "$prefix$color$timeDisplay$suffix"
    }
}