package com.github.noamm9.features.impl.dungeon.solvers.devices

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.NoammRenderLayers
import com.github.noamm9.utils.render.RenderContext
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object SimonSays: Feature("Simon Says Solver") {
    private val ssSkip by ToggleSetting("SS skip Compatibility").withDescription("Always assume at the start that you perfectly SS skip").section("Options")
    private val blockWrongClicks by ToggleSetting("Block Wrong Clicks").withDescription("Blocks clicks if you aren't looking at the correct button. &eSneak to override.")

    private val color1 by ColorSetting("First Color", Color.GREEN).withDescription("Color of the first button.").section("Colors")
    private val color2 by ColorSetting("Second Color", Color.YELLOW).withDescription("Color of the second button.")
    private val color3 by ColorSetting("Other Color", Color.RED).withDescription("Color of the rest of the buttons.")

    //#if CHEAT
    private val autoStart by ToggleSetting("Auto Start", false).withDescription("Automatically starts the device when it can be started.").section("Auto")
    private val startClicks by SliderSetting("Start Clicks", 3, 1, 10, 1).withDescription("Amount of clicks to start the device.").showIf { autoStart.value }
    private val startClickDelay by SliderSetting("Start Click Delay", 3, 1, 25, 1).withDescription("Delay in ticks between each start click.").showIf { autoStart.value }

    private val autoSS by ToggleSetting("Auto SS").withDescription("Automatically does the device.").showIf { NoammAddons.debugFlags.contains("autoss") }
    private val autoSSDelay by SliderSetting("Auto SS delay", 3, 1, 10, 1)
        .withDescription("Delay in Server ticks.").showIf { autoSS.value && NoammAddons.debugFlags.contains("autoss") }
    //#endif

    private val alertsEnabled by ToggleSetting("Alerts Enabled", true).section("Alerts")
    private val sendChat by ToggleSetting("SS Break Alert", true).showIf { alertsEnabled.value }.withDescription("Sends in party chat when the device got reset")
    private val sendRestartChat by ToggleSetting("Send Restart Chat", true).showIf { alertsEnabled.value }.withDescription("Sends a message in party chat when you restart the device")
    private val alertSound by ToggleSetting("Alert Sound", true).showIf { alertsEnabled.value }.withDescription("Plays a sound when the device fails")
    private val showTitle by ToggleSetting("Show Title", true).showIf { alertsEnabled.value }.withDescription("Shows a title when the device fails")

    private val solution = ArrayList<BlockPos>()
    private var lastExisted = false
    private var skipOver = false
    private var allObi = true
    private var lastClick = 0L

    private val startButton = BlockPos(110, 121, 91)
    private val startRegex = Regex("^\\[BOSS] Goldor: Who dares trespass into my domain\\?$")

    private val buttonCheckPos = BlockPos(110, 120, 92)
    private val startPos = BlockPos(111, 120, 92)

    private var thingsDone = 0
    private var ticks = 0
    private var canBreak = false
    private var wasBroken = false

    private val obsidians = (120 .. 123).flatMap { y -> (92 .. 95).map { z -> BlockPos(111, y, z) } }
    private val buttons = (120 .. 123).flatMap { y -> (92 .. 95).map { z -> BlockPos(110, y, z) } }

    private val deviceRegex = Regex("(.+) (activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)")


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

        register<TickEvent.Start> {
            if (LocationUtils.F7Phase != 3) return@register
            val buttonsExist = WorldUtils.getBlockAt(buttonCheckPos) == Blocks.STONE_BUTTON

            if (buttonsExist && ! lastExisted) {
                allObi = true

                for (dy in 0 .. 3) for (dz in 0 .. 3) {
                    val pos = startPos.offset(0, dy, dz)
                    if (WorldUtils.getBlockAt(pos) != Blocks.OBSIDIAN) {
                        allObi = false
                    }
                }


                if (allObi) {
                    lastExisted = true
                    skipOver = true

                    //#if CHEAT
                    if (autoSS.value && NoammAddons.debugFlags.contains("autoss")) scope.launch {
                        val list = solution.toList()
                        for (pos in list) {
                            val targetTick = DungeonListener.currentTime + autoSSDelay.value
                            PlayerUtils.rotateSmoothly(pos.west().center, autoSSDelay.value * 50L)
                            if (list.first() == pos) PlayerUtils.rightClick()

                            while (DungeonListener.currentTime < targetTick) Thread.sleep(10)
                            if (list.first() != pos) PlayerUtils.rightClick()
                        }
                    }
                    //#endif
                }
            }

            if (! buttonsExist && lastExisted) {
                lastExisted = false
                solution.clear()
            }

            for (dy in 0 .. 3) for (dz in 0 .. 3) {
                val pos = startPos.offset(0, dy, dz)
                val block = WorldUtils.getBlockAt(pos)
                if (block != Blocks.SEA_LANTERN || solution.contains(pos)) continue

                if (solution.contains(pos)) solution.remove(pos)
                solution.add(pos)

                if (! skipOver && ssSkip.value && solution.size == 3) {
                    solution.removeAt(0)
                }
            }
        }

        register<RenderWorldEvent> {
            if (LocationUtils.F7Phase != 3) return@register
            if (solution.isEmpty()) return@register

            for (i in solution.indices) {
                val color = when (i) {
                    0 -> color1
                    1 -> color2
                    else -> color3
                }.value

                renderSSBox(event.ctx, solution[i].west(), color)
            }
        }

        fun handleClick(event: PlayerInteractEvent, clickedPos: BlockPos) {
            if (LocationUtils.F7Phase != 3) return

            if (clickedPos.x == 110 && clickedPos.y == 121 && clickedPos.z == 91) {
                solution.clear()
                skipOver = false
                return
            }

            if (solution.isEmpty()) return
            if (WorldUtils.getBlockAt(clickedPos) != Blocks.STONE_BUTTON) return
            if (lastClick == DungeonListener.currentTime) return event.cancel()
            lastClick = DungeonListener.currentTime

            val checkPos = clickedPos.east()
            val expected = solution.firstOrNull() ?: return

            if (checkPos != expected) {
                if (blockWrongClicks.value && ! mc.player !!.isCrouching) return event.cancel()

                if (solution.size == 3 && checkPos == solution[1]) {
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

    val serverTickListener = EventBus.register<TickEvent.Server> {
        ticks --

        if (obsidians.any { WorldUtils.getBlockAt(it) != Blocks.OBSIDIAN }) {
            ticks = 12
            canBreak = true

            if (wasBroken) {
                wasBroken = false
                if (sendRestartChat.value) ChatUtils.sendCommand("pc SS Started Again!")
                if (showTitle.value) ChatUtils.showTitle("§a§l§nSS Started!")
            }

            return@register
        }

        if (ticks > 0 || ! canBreak) return@register
        if (! buttons.all { pos -> WorldUtils.getBlockAt(pos) == Blocks.AIR }) return@register

        canBreak = false
        wasBroken = true

        if (sendChat.value) ChatUtils.sendCommand("pc SS Broke!")
        if (alertSound.value) mc.player?.playSound(SoundEvents.ANVIL_LAND, 5f, 0f)
        if (showTitle.value) ChatUtils.showTitle("§c§l§nSS BROKE!")
    }.unregister()

    private fun resetSolver() {
        lastExisted = false
        skipOver = false
        solution.clear()
        allObi = true
    }

    private fun reset() {
        serverTickListener.unregister()
        thingsDone = 0
        ticks = 0
        canBreak = false
        wasBroken = false
    }

    private fun renderSSBox(ctx: RenderContext, pos: BlockPos, color: Color) {
        val cam = ctx.camera.position.reverse()

        val w = 0.4 / 2.0
        val h = 0.26 / 2.0

        val cx = pos.x + 1.0
        val cy = pos.y + 0.5
        val cz = pos.z + 0.5

        val minX = cx - 0.2
        val minY = cy - h
        val maxY = cy + h
        val minZ = cz - w
        val maxZ = cz + w

        ctx.matrixStack.pushPose()
        ctx.matrixStack.translate(cam.x, cam.y, cam.z)

        ShapeRenderer.addChainedFilledBoxVertices(
            ctx.matrixStack,
            ctx.consumers.getBuffer(NoammRenderLayers.FILLED_THROUGH_WALLS),
            minX, minY, minZ,
            cx, maxY, maxZ,
            color.red / 255f, color.green / 255f, color.blue / 255f, 0.7f
        )

        ctx.matrixStack.popPose()
    }
}

/*
object SimonSaysAlert: Feature("SS Alert") {
    private val alertsEnabled by ToggleSetting("Alerts Enabled", true).section("Alerts")
    private val sendChat by ToggleSetting("Send Party Chat", true).showIf { alertsEnabled.value }
    private val sendRestartChat by ToggleSetting("Send Restart Chat", true).showIf { alertsEnabled.value }
    private val alertSound by ToggleSetting("Alert Sound", true).showIf { alertsEnabled.value }
    private val showTitle by ToggleSetting("Show Title", true).showIf { alertsEnabled.value }

    private var thingsDone = 0
    private var ticks = 0
    private var allowBreak = false
    private var hasBroken = false
    private var isListening = false

    private val obsidians = (120 .. 123).flatMap { y -> (92 .. 95).map { z -> BlockPos(111, y, z) } }
    private val buttons = (120 .. 123).flatMap { y -> (92 .. 95).map { z -> BlockPos(110, y, z) } }

    private val goldorStartRegex = Regex("\\[BOSS] Goldor: Who dares trespass into my domain\\?")
    private val deviceRegex = Regex("(.+) (activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)")

    override fun init() {
        register<WorldChangeEvent> { reset() }

        register<ChatMessageEvent> {
            val msg = event.unformattedText

            if (goldorStartRegex.matches(msg)) {
                reset()
                isListening = true
                return@register
            }

            if (! isListening) return@register

            val (_, _, type, completedStr, _) = deviceRegex.find(msg)?.destructured ?: return@register
            val completed = completedStr.toIntOrNull() ?: 0

            when (type) {
                "terminal", "lever" -> thingsDone ++
                "device" -> {
                    if ((thingsDone + 1) >= completed) {
                        isListening = false
                    }
                }
            }
        }

        register<TickEvent.Server> {
            if (! isListening || mc.level == null) return@register
            ticks --

            val isGameActive = obsidians.any { pos ->
                WorldUtils.getBlockAt(pos) != Blocks.OBSIDIAN
            }

            if (isGameActive) {
                ticks = 12
                allowBreak = true

                if (hasBroken) {
                    hasBroken = false
                    if (sendRestartChat.value) ChatUtils.sendCommand("pc SS Started Again!")
                    if (showTitle.value) {
                        ChatUtils.showTitle("§a§l§nSS Started!")
                    }
                }
                return@register
            }

            if (ticks > 0 || ! allowBreak) return@register

            val allButtonsMissing = buttons.all { pos ->
                WorldUtils.getBlockAt(pos) == Blocks.AIR
            }

            if (! allButtonsMissing) return@register

            allowBreak = false
            hasBroken = true

            if (sendChat.value) ChatUtils.sendCommand("pc SS Broke!")

            if (alertSound.value) {
                mc.player?.playSound(SoundEvents.ANVIL_LAND, 5f, 0f)
            }

            if (showTitle.value) {
                ChatUtils.showTitle("§c§l§nSS BROKE!")
            }
        }
    }

    private fun reset() {
        isListening = false
        thingsDone = 0
        ticks = 0
        allowBreak = false
        hasBroken = false
    }
}*/