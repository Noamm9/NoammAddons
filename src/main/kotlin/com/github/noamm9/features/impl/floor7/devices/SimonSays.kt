package com.github.noamm9.features.impl.floor7.devices

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.utils.*
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.MathUtils.vec
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.world.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.world.Render3D.renderString
import com.github.noamm9.utils.render.world.RenderContext
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.*

object SimonSays: Feature("Simon Says Solver") {
    private val progressDisplay by ToggleSetting("Progress Display").withDescription("Displays the current Simon Says stage.").section("HUD")

    private val ssSkip by ToggleSetting("SS skip Compatibility", true).withDescription("Always assume at the start that you perfectly SS skip").section("Options")
    private val blockWrongClicks by ToggleSetting("Block Wrong Clicks").withDescription("Blocks clicks if you aren't looking at the correct button. &eSneak to override.")
    private val color1 by ColorSetting("First Color", Color.GREEN).withDescription("Color of the first button.")
    private val color2 by ColorSetting("Second Color", Color.YELLOW).withDescription("Color of the second button.")
    private val color3 by ColorSetting("Other Color", Color.RED).withDescription("Color of the rest of the buttons.")
    private val outline by ToggleSetting("Outline", false).withDescription("Renders the box with an outline.")
    private val phase by ToggleSetting("Phase", true).withDescription("Renders the box through walls.")

    //#if CHEAT
    private val triggerBot by ToggleSetting("Triggerbot", false).withDescription("Automatically clicks the correct button when you're aiming at it.").section("Auto")
    private val autoStart by ToggleSetting("Auto Start", false).withDescription("Automatically starts the device when it can be started.")
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
    private val deviceCenter = vec(110.5, 121.5, 93.5)

    private val lastKnownPositions = HashMap<String, Vec3>()
    private val solution = ArrayList<SSButton>()
    private var skipOver = false
    private var lastClick = 0L
    private var sequenceLength = 0
    private var stage = 0

    private var thingsDone = 0
    private var ticks = 0
    private var canBreak = false
    private var wasBroken = false

    private var startTick = 0L

    override fun init() {
        hudElement(
            name = "Simon Says Progress",
            enabled = { progressDisplay.value },
            shouldDraw = { LocationUtils.F7Phase == 3 && stage > 0 }
        ) { ctx, example ->
            val displayedStage = if (example) 3 else stage
            val color = ColorUtils.colorCodeByPercent(displayedStage, 5)
            val text = "&7Simon Says: $color$displayedStage&7/&a5"

            ctx.drawString(text, 0, 0)
            text.width().toFloat() to 9f
        }

        register<WorldChangeEvent> {
            resetSolver()
            reset()
        }

        register<TickEvent.Start> {
            if (LocationUtils.F7Phase != 3) return@register
            level.players().forEach { lastKnownPositions[it.gameProfile.name] = it.position() }
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

        register<TickEvent.Start> {
            if (! triggerBot.value) return@register
            if (LocationUtils.F7Phase != 3) return@register
            if (lastClick + 1 >= DungeonListener.currentTime) return@register

            val expected = solution.firstOrNull() ?: return@register
            if (PlayerUtils.getSelectionBlock() != expected.button) return@register

            PlayerUtils.rightClick()
        }
        //#endif

        register<BlockChangeEvent> {
            if (event.pos !in obsidians) return@register
            if (event.newBlock != Blocks.SEA_LANTERN) return@register
            if (ssSkip.value && solution.size == 2 && ! skipOver) {
                solution.removeFirst()
                sequenceLength --
            }
            solution.add(SSButton(event.pos))
            sequenceLength = (sequenceLength + 1).coerceAtMost(5)
        }

        register<BlockChangeEvent> {
            if (event.pos != buttonCheckPos) return@register
            if (event.newBlock == Blocks.AIR) {
                sequenceLength = 0
                return@register
            }
            if (event.newBlock != Blocks.STONE_BUTTON) return@register

            skipOver = true
            if (sequenceLength > 0) stage = maxOf(stage, sequenceLength)
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

                event.ctx.renderSSBox(buttonPos, color)
                if (NoammAddons.debugFlags.contains("ss")) event.ctx.renderString("$id", buttonPos.toVec().add(x = 0.8, y = 0.6, z = 0.5), phase = true)
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
                if (blockWrongClicks.value && ! player.isCrouching) return event.cancel()

                if (solution.size == 3 && clickedPos == solution[1].button) {
                    for (i in 1 downTo 0) solution.removeAt(i)
                }
            }
            else solution.remove(expected)
        }

        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> { handleClick(event, event.pos) }
        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> { handleClick(event, event.pos) }

        register<ChatMessageEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            val msg = event.unformattedText

            if (startRegex.matches(msg)) {
                resetSolver()
                reset()
                serverTickListener.register()
                startTick = DungeonListener.currentTime
                return@register
            }

            val (name, _, type, completedStr, _) = deviceRegex.find(msg)?.destructured ?: return@register
            val completed = completedStr.toIntOrNull() ?: 0

            if (type == "device") {
                val position = level.players().find { it.gameProfile.name == name }?.position() ?: lastKnownPositions[name]
                if (position != null && position.distanceToSqr(deviceCenter) <= 25) {
                    resetSolver()
                    val time = (DungeonListener.currentTime - startTick) / 20.0
                    ChatUtils.modMessage("&bSimon Says Took §e${time.toFixed(2)}s&b to complate!")
                }
            }

            if (! serverTickListener.isActive) return@register

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

    val serverTickListener = EventBus.listener<TickEvent.Server> {
        ticks --

        if (obsidians.any { WorldUtils.getBlockAt(it) != Blocks.OBSIDIAN }) {
            ticks = 12
            canBreak = true

            if (wasBroken) {
                wasBroken = false
                if (alertsEnabled.value) {
                    if (sendRestartChat.value) ChatUtils.sendCommand("pc SS Started Again!")
                    if (showTitle.value) ChatUtils.showTitle("§a§l§nSS Started!")
                }
            }

            return@listener
        }

        if (ticks > 0 || ! canBreak) return@listener
        if (! buttons.all { pos -> WorldUtils.getBlockAt(pos) == Blocks.AIR }) return@listener

        canBreak = false
        wasBroken = true

        if (alertsEnabled.value) {
            if (alertSound.value) ThreadUtils.scheduledTask { player.playSound(SoundEvents.ANVIL_LAND, 5f, 0f) }
            if (showTitle.value) ChatUtils.showTitle("§c§l§nSS BROKE!")
            if (sendChat.value) ChatUtils.sendCommand("pc SS Broke!")
        }

        resetSolver()
    }

    override fun onDisable() {
        super.onDisable()
        resetSolver()
        reset()
    }

    private fun resetSolver() {
        solution.clear()
        skipOver = false
        sequenceLength = 0
        stage = 0
        lastClick = 0
    }

    private fun reset() {
        serverTickListener.unregister()
        lastKnownPositions.clear()
        thingsDone = 0
        ticks = 0
        canBreak = false
        wasBroken = false
        startTick = 0L
    }

    private fun RenderContext.renderSSBox(pos: BlockPos, color: Color) {
        val state = level.getBlockState(pos)
        var depth = if (state.block == Blocks.STONE_BUTTON && state.getValue(ButtonBlock.POWERED)) 1.0 else 2.0
        depth /= 16

        renderBoxBounds(
            pos.x + 1 - depth,
            pos.y + 0.375, pos.z + 0.3125, pos.x + 1.0,
            pos.y + 0.625, pos.z + 0.6875, color,
            outline = outline.value,
            phase = phase.value
        )
    }

    private class SSButton(obsidian: BlockPos) {
        val button = obsidian.west()
        val id = solution.size
    }
}