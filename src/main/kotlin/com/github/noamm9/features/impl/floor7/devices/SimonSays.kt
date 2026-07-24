package com.github.noamm9.features.impl.floor7.devices

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventListener
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ButtonBlock
import java.awt.Color
import java.util.*

object SimonSays: Feature("Simon Says Solver") {
    private val ssSkip by ToggleSetting("SS skip Compatibility", true).withDescription("Always assume at the start that you perfectly SS skip").section("Options")
    private val blockWrongClicks by ToggleSetting("Block Wrong Clicks").withDescription("Blocks clicks if you aren't looking at the correct button. &eSneak to override.")
    private val color1 by ColorSetting("First Color", Color.GREEN).withDescription("Color of the first button.")
    private val color2 by ColorSetting("Second Color", Color.YELLOW).withDescription("Color of the second button.")
    private val color3 by ColorSetting("Other Color", Color.RED).withDescription("Color of the rest of the buttons.")

    //#if CHEAT
    private val autoStart by ToggleSetting("Auto Start", false).withDescription("Automatically starts the device when it can be started.").section("Auto")
    private val startClicks by SliderSetting("Start Clicks", 3, 1, 10, 1).withDescription("Amount of clicks to start the device.").showIf { autoStart.value }
    private val startClickDelay by SliderSetting("Start Click Delay", 3, 1, 25, 1).withDescription("Delay in ticks between each start click.").showIf { autoStart.value }
    //#endif

    private val alertsEnabled by ToggleSetting("Alerts Enabled", true).section("Alerts")
    private val sendChat by ToggleSetting("SS Break Alert", true).showIf { alertsEnabled.value }.withDescription("Sends in party chat when the device got reset")
    private val sendRestartChat by ToggleSetting("Send Restart Chat", true).showIf { alertsEnabled.value }.withDescription("Sends a message in party chat when you restart the device")
    private val alertSound by ToggleSetting("Alert Sound", true).showIf { alertsEnabled.value }.withDescription("Plays a sound when the device fails")
    private val showTitle by ToggleSetting("Show Title", true).showIf { alertsEnabled.value }.withDescription("Shows a title when the device fails")

    private val deviceRegex = Regex("(.+) (activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)")
    private val startRegex = Regex("^\\[BOSS] Goldor: Who dares trespass into my domain\\?$")
    private val obsidians = (120 .. 123).flatMap { y -> (92 .. 95).map { z -> BlockPos(111, y, z) } }
    private val buttons = (120 .. 123).flatMap { y -> (92 .. 95).map { z -> BlockPos(110, y, z) } }

    private val buttonCheckPos = BlockPos(110, 120, 93)
    private val startButton = BlockPos(110, 121, 91)

    private val solution = ArrayList<SSButton>()
    private var skipOver = false
    private var lastClick = 0L

    private var thingsDone = 0
    private var ticks = 0
    private var canBreak = false
    private var wasBroken = false

    override fun init() {
        register<WorldChangeEvent> {
            resetSolver()
            reset()
        }

        //#if CHEAT
        register<ChatMessageEvent> {
            if (! autoStart.value) return@register
            if (LocationUtils.F7Phase != 3) return@register
            if (! event.unformattedText.matches(startRegex)) return@register
            if (PlayerUtils.getSelectionBlock() == startButton) repeat(startClicks.value) {
                ThreadUtils.scheduledTask(it * startClickDelay.value) {
                    PlayerUtils.rightClick()
                }
            }
        }
        //#endif

        register<BlockChangeEvent> {
            val pos = event.pos.immutable()
            if (pos !in obsidians) return@register
            if (event.newBlock != Blocks.SEA_LANTERN) return@register
            if (ssSkip.value && solution.size == 2 && ! skipOver) solution.removeFirst()
            solution.add(SSButton(pos))
        }

        register<BlockChangeEvent> {
            if (event.pos != buttonCheckPos) return@register
            if (event.newBlock != Blocks.STONE_BUTTON) return@register
            skipOver = true
        }

        register<RenderWorldEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            if (solution.isEmpty()) return@register

            for (i in solution.indices) {
                val buttonPos = solution[i].button
                val id = solution[i].id
                val color = when (i) {
                    0 -> color1
                    1 -> color2
                    else -> color3
                }.value

                renderSSBox(event.ctx, buttonPos, color)
                if (NoammAddons.debugFlags.contains("ss")) Render3D.renderString("$id", buttonPos.toVec().add(x = 0.8, y = 0.6, z = 0.5), phase = true)
            }
        }

        fun handleClick(event: PlayerInteractEvent, clickedPos: BlockPos) {
            if (LocationUtils.F7Phase != 3) return
            if (clickedPos == startButton) return resetSolver()
            if (solution.isEmpty()) return
            if (WorldUtils.getBlockAt(clickedPos) != Blocks.STONE_BUTTON) return
            if (lastClick == DungeonListener.currentTime) return event.cancel()
            lastClick = DungeonListener.currentTime

            val expected = solution.firstOrNull() ?: return

            if (clickedPos != expected.button) {
                if (blockWrongClicks.value && ! mc.player !!.isCrouching) return event.cancel()

                if (solution.size == 3 && clickedPos == solution[1].button) {
                    for (i in 1 downTo 0) solution.removeAt(i)
                }
            }
            else solution.remove(expected)
        }

        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> { handleClick(event, event.pos) }
        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> { handleClick(event, event.pos) }

        register<ChatMessageEvent> {
            if (! alertsEnabled.value) return@register
            if (LocationUtils.F7Phase != 3) return@register
            val msg = event.unformattedText

            if (startRegex.matches(msg)) {
                reset()
                serverTickListener.register()
                return@register
            }

            if (! serverTickListener.isRegistered()) return@register

            val (_, _, type, completedStr, _) = deviceRegex.find(msg)?.destructured ?: return@register
            val completed = completedStr.toIntOrNull() ?: 0

            when (type) {
                "terminal", "lever" -> thingsDone ++
                "device" -> {
                    if ((thingsDone + 1) >= completed) {
                        serverTickListener.unregister()
                    }
                }
            }
        }
    }

    val serverTickListener = EventListener.create<TickEvent.Server> {
        ticks --

        if (obsidians.any { WorldUtils.getBlockAt(it) != Blocks.OBSIDIAN }) {
            ticks = 12
            canBreak = true

            if (wasBroken) {
                wasBroken = false
                if (sendRestartChat.value) ChatUtils.sendCommand("pc SS Started Again!")
                if (showTitle.value) ChatUtils.showTitle("§a§l§nSS Started!")
            }

            return@create
        }

        if (ticks > 0 || ! canBreak) return@create
        if (! buttons.all { pos -> WorldUtils.getBlockAt(pos) == Blocks.AIR }) return@create

        canBreak = false
        wasBroken = true

        if (sendChat.value) ChatUtils.sendCommand("pc SS Broke!")
        if (alertSound.value) ThreadUtils.scheduledTask { mc.player?.playSound(SoundEvents.ANVIL_LAND, 5f, 0f) }
        if (showTitle.value) ChatUtils.showTitle("§c§l§nSS BROKE!")

        resetSolver()
    }

    private fun resetSolver() {
        solution.clear()
        skipOver = false
    }

    private fun reset() {
        serverTickListener.unregister()
        thingsDone = 0
        ticks = 0
        canBreak = false
        wasBroken = false
    }

    private fun renderSSBox(ctx: RenderContext, pos: BlockPos, color: Color) {
        val state = mc.level?.getBlockState(pos)
        var depth = if (state?.block == Blocks.STONE_BUTTON && state.getValue(ButtonBlock.POWERED)) 1.0 else 2.0
        depth /= 16

        Render3D.renderBoxBounds(
            ctx,
            pos.x + 1 - depth, pos.y + 0.375, pos.z + 0.3125,
            pos.x + 1.0, pos.y + 0.625, pos.z + 0.6875,
            color,
            outline = false,
            phase = true
        )
    }

    private class SSButton(obsidian: BlockPos) {
        val button = obsidian.west()
        val id = solution.size
    }
}