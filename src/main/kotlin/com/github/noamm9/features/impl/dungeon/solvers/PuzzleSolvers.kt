package com.github.noamm9.features.impl.dungeon.solvers

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolver
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import java.awt.Color

object PuzzleSolvers: Feature() {
    val blaze by ToggleSetting("Enabled").section("Blaze Solver")
    val blazeCount by SliderSetting("Blazes to Highlight", 3, 1, 10, 1).showIf { blaze.value }
    val firstBlazeColor by ColorSetting("First Blaze", Color.GREEN).showIf { blaze.value }
    val secondBlazeColor by ColorSetting("Second Blaze", Color.YELLOW).showIf { blaze.value }
    val thirdBlazeColor by ColorSetting("Other Blazes", Color.RED).showIf { blaze.value }
    val lineColor by ColorSetting("Line Color", Color.WHITE).showIf { blaze.value }

    val boulder by ToggleSetting("Enabled ").section("Boulder Solver")
    val showAll by ToggleSetting("Show All Moves", false).showIf { boulder.value }
    val boxColor by ColorSetting("Boulder Box Color", Color.BLUE.withAlpha(100)).showIf { boulder.value }
    val clickColor by ColorSetting("Click Color", Color.RED.withAlpha(100)).showIf { boulder.value }

    val creeper by ToggleSetting("Enabled  ").section("Creeper Beam Solver")
    val renderLines by ToggleSetting("Render Lines", true).showIf { creeper.value }
    val phase by ToggleSetting("Phase Through Walls", true).showIf { creeper.value }

    val quiz by ToggleSetting("Enabled   ").section("Quiz Solver")
    val answerColor by ColorSetting("Answer Color", Color.CYAN.withAlpha(128)).showIf { quiz.value }
    val quizTimer by ToggleSetting("Quiz Timer", true).showIf { quiz.value }

    val tpmaze by ToggleSetting("Enabled    ").section("Teleport Maze Solver")
    val correctTpPadColor by ColorSetting("Correct Pad Color", Color.GREEN).showIf { tpmaze.value }
    val wrongTpPadColor by ColorSetting("Wrong Pad Color", Color.RED).showIf { tpmaze.value }

    val weirdos by ToggleSetting("Enabled     ").section("Three Weirdos Solver")
    val removeChests by ToggleSetting("Hide Wrong Chests", true).showIf { weirdos.value }
    val colorCorrect by ColorSetting("Correct Chest Color", Color.GREEN).showIf { weirdos.value }
    val colorWrong by ColorSetting("Wrong Chest Color", Color.RED).showIf { weirdos.value }

    val ttt by ToggleSetting("Enabled      ").section("TicTacToe Solver")
    val preventMissClick by ToggleSetting("Prevent Miss Click", true).showIf { ttt.value }
    val color by ColorSetting("Highlight Color", Color.GREEN).showIf { ttt.value }
    val prediction by ToggleSetting("Prediction").showIf { ttt.value }
    val predictionColor by ColorSetting("Prediction Color", Color.ORANGE).showIf { ttt.value && prediction.value }

    val water by ToggleSetting("Enabled       ").section("Water Board Solver")
    val currentClickColor by ColorSetting("Click Color", Color.GREEN).showIf { water.value }
    val nextColor by ColorSetting("Next Click Color", Color.YELLOW).showIf { water.value }

    val icefill by ToggleSetting("Enabled        ").section("Ice Fill Solver")
    val icefillColor by ColorSetting("Click Color", Color.GREEN).showIf { icefill.value }

    val icepath by ToggleSetting("Enabled         ").section("Ice Path Solver")
    val icePathFirstColor by ColorSetting("Next Segment Color", Color.GREEN).showIf { icepath.value }
    val icePathColor by ColorSetting("Segments Color", Color.RED).showIf { icepath.value }

    override fun init() {
        val puzzles = PuzzleSolver::class.sealedSubclasses.mapNotNull { it.objectInstance }

        register<DungeonEvent.RoomEvent.onStateChange> { puzzles.forEach { if (it.enabled) it.onStateChange(event) } }
        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> { puzzles.forEach { if (it.enabled) it.onInteract(event) } }
        register<RenderOverlayEvent> { puzzles.forEach { if (it.enabled) it.onRenderOverlay(event.context) } }
        register<MainThreadPacketReceivedEvent.Pre> { puzzles.forEach { if (it.enabled) it.onPacket(event) } }
        register<DungeonEvent.RoomEvent.onEnter> { puzzles.forEach { if (it.enabled) it.onRoomEnter(event) } }
        register<RenderWorldEvent> { puzzles.forEach { if (it.enabled) it.onRenderWorld(event.ctx) } }
        register<CheckEntityGlowEvent> { puzzles.forEach { if (it.enabled) it.onEntityGlow(event) } }
        register<DungeonEvent.RoomEvent.onExit> { puzzles.forEach { if (it.enabled) it.onRoomExit() } }
        register<ChatMessageEvent> { puzzles.forEach { if (it.enabled) it.onChat(event) } }
        register<TickEvent.Server> { puzzles.forEach { if (it.enabled) it.onTick() } }
        register<WorldChangeEvent> { puzzles.forEach { if (it.enabled) it.reset() } }
    }
}