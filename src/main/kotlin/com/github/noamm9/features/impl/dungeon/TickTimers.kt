package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.network.protocol.game.ClientboundSetTimePacket

object TickTimers: Feature("Shows various types of server tick timers for F7 boss fight.") {
    private val showPrefix by ToggleSetting("Prefix", true).section("Settings")
    private val showSuffix by ToggleSetting("Suffix", true)
    private val format by DropdownSetting("Format", 0, listOf("Seconds", "Ticks"))

    private val deathTickTimer by ToggleSetting("0s Death Tick").section("clear")
    private val secretTickTimer by ToggleSetting("SecretTick")

    private val p1 by ToggleSetting("Maxor Start").section("F7")
    private val p2 by ToggleSetting("Storm Start")
    private val p3 by ToggleSetting("Goldor Start")
    private val p4 by ToggleSetting("Necron Start")

    private val goldorDeathTickTimer by ToggleSetting("Goldor Death Ticks")
    private val padTimer by ToggleSetting("Storm Pad Timer")
    private val pyTimer by ToggleSetting("Storm PY Timer")

    private var startTickTime = - 1
    private var goldorTickTime = - 1
    private var padTickTime = - 1
    private var pyTickTime = - 1
    private var stormActive = false
    private var pyTriggered = false

    private var deathTickTime = - 1
    private var secretTickTime = - 1
    private var dungeonStartTime = 0L

    override fun init() {
        hudElement("Tick Timers", shouldDraw = { LocationUtils.inDungeon }, centered = true) { ctx, example ->
            val textToRender = if (example) "§aStart: 150"
            else when {
                startTickTime != - 1 -> formatTimer(startTickTime, 150, "§aStart:")
                goldorTickTime != - 1 -> formatTimer(goldorTickTime, 60, "§7Goldor:")
                padTickTime != - 1 -> formatTimer(padTickTime, 20, "§bPad:")
                pyTickTime != - 1 -> formatTimer(pyTickTime, 95, "§5PY:")
                deathTickTime != - 1 -> formatTimer(deathTickTime, 40, "§cDeath:")
                secretTickTime != - 1 -> formatTimer(secretTickTime, 20, "§dSecret:")
                else -> return@hudElement 0f to 0f
            }

            Render2D.drawCenteredString(ctx, textToRender, 0f, 0f)
            return@hudElement textToRender.width().toFloat() to 9F
        }

        register<WorldChangeEvent> { reset() }
        register<DungeonEvent.RunStatedEvent> { dungeonStartTime = System.currentTimeMillis() }

        register<ChatMessageEvent> {
            when (event.unformattedText) {
                "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" -> if (p1.value) startTickTime = 167

                "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!" -> if (p2.value) startTickTime = 120

                "[BOSS] Storm: ENERGY HEED MY CALL!", "[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!" -> {
                    if (pyTimer.value && ! pyTriggered) {
                        pyTriggered = true
                        pyTickTime = 95
                    }
                }

                "[BOSS] Storm: I should have known that I stood no chance." -> {
                    if (p3.value) startTickTime = 104
                    if (stormActive) {
                        stormActive = false
                        padTickTime = - 1
                    }
                    if (pyTriggered) {
                        pyTriggered = false
                        pyTickTime = - 1
                    }
                }

                "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                    if (goldorDeathTickTimer.value) goldorTickTime = 60
                }

                "[BOSS] Necron: I'm afraid, your journey ends now." -> if (p4.value) startTickTime = 60

                "The Core entrance is opening!" -> goldorTickTime = - 1

                "[BOSS] Storm: Pathetic Maxor, just like expected." -> {
                    if (padTimer.value) {
                        padTickTime = 20
                        stormActive = true
                    }
                }
            }
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! LocationUtils.inDungeon) return@register
            if (event.packet !is ClientboundSetTimePacket) return@register

            val timeSinceStart = System.currentTimeMillis() - dungeonStartTime
            val shouldCheckDeath = deathTickTimer.value && (timeSinceStart < 6000 || ! DungeonListener.dungeonStarted)

            if (shouldCheckDeath) deathTickTime = 40 - (event.packet.gameTime % 40).toInt()
            else if (! LocationUtils.inBoss) {
                if (secretTickTimer.value) {
                    secretTickTime = 20 - (event.packet.gameTime % 20).toInt()
                    deathTickTime = - 1
                }
                else {
                    secretTickTime = - 1
                    deathTickTime = - 1
                }
            }
        }

        register<TickEvent.Server> {
            if (startTickTime != - 1) startTickTime --

            if (stormActive && padTickTime != - 1) {
                padTickTime --
                if (padTickTime <= 0) padTickTime = 20
            }

            if (pyTimer.value && pyTickTime >= 0) {
                pyTickTime --
            }

            if (goldorTickTime >= 0) {
                goldorTickTime --
                if (goldorTickTime == 0) goldorTickTime = 60
            }

            if (deathTickTimer.value && deathTickTime >= 0) {
                deathTickTime --
                if (deathTickTime == 0) deathTickTime = 40
            }

            if (secretTickTimer.value && secretTickTime >= 0) {
                secretTickTime --
                if (secretTickTime == 0 && ! LocationUtils.inBoss) secretTickTime = 20
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
        deathTickTime = - 1
        secretTickTime = - 1
        dungeonStartTime = 0L
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