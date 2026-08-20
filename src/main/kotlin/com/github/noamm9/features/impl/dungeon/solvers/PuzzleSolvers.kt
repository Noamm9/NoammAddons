package com.github.noamm9.features.impl.dungeon.solvers

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolver
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.QuizSolver
import com.github.noamm9.config.types.ColorConfig
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.width
import java.awt.Color

object PuzzleSolvers: Feature() {
    val blaze by BooleanConfig("Enabled").section("Blaze Solver")
    val blazeCount by NumberConfig("Blazes to Highlight", 3, 1, 10, 1).showIf { blaze.value }
    val firstBlazeColor by ColorConfig("First Blaze", Color.GREEN).showIf { blaze.value }
    val secondBlazeColor by ColorConfig("Second Blaze", Color.YELLOW).showIf { blaze.value }
    val thirdBlazeColor by ColorConfig("Other Blazes", Color.RED).showIf { blaze.value }
    val lineColor by ColorConfig("Line Color", Color.WHITE).showIf { blaze.value }

    val boulder by BooleanConfig("Enabled ").section("Boulder Solver")
    val showAll by BooleanConfig("Show All Moves", false).showIf { boulder.value }
    val boxColor by ColorConfig("Boulder Box Color", Color.BLUE.withAlpha(100)).showIf { boulder.value }
    val clickColor by ColorConfig("Click Color", Color.RED.withAlpha(100)).showIf { boulder.value }

    val creeper by BooleanConfig("Enabled  ").section("Creeper Beam Solver")
    val renderLines by BooleanConfig("Render Lines", true).showIf { creeper.value }
    val phase by BooleanConfig("Phase Through Walls", true).showIf { creeper.value }

    val quiz by BooleanConfig("Enabled   ").section("Quiz Solver")
    val answerColor by ColorConfig("Answer Color", Color.CYAN.withAlpha(128)).showIf { quiz.value }
    val quizTimer by BooleanConfig("Quiz Timer", true).showIf { quiz.value }
    val quizBlockWrongClicks by BooleanConfig("Block Wrong Clicks", true)
        .withDescription("Prevents clicking a wrong Quiz answer. &eSneak to override.")
        .showIf { quiz.value }

    val tpmaze by BooleanConfig("Enabled    ").section("Teleport Maze Solver")
    val correctTpPadColor by ColorConfig("Correct Pad Color", Color.GREEN).showIf { tpmaze.value }
    val wrongTpPadColor by ColorConfig("Wrong Pad Color", Color.RED).showIf { tpmaze.value }

    val weirdos by BooleanConfig("Enabled     ").section("Three Weirdos Solver")
    val removeChests by BooleanConfig("Hide Wrong Chests", true).showIf { weirdos.value }
    val colorCorrect by ColorConfig("Correct Chest Color", Color.GREEN).showIf { weirdos.value }
    val colorWrong by ColorConfig("Wrong Chest Color", Color.RED).showIf { weirdos.value }

    val ttt by BooleanConfig("Enabled      ").section("TicTacToe Solver")
    val preventMissClick by BooleanConfig("Prevent Miss Click", true).showIf { ttt.value }
    val color by ColorConfig("Highlight Color", Color.GREEN).showIf { ttt.value }
    val prediction by BooleanConfig("Prediction").showIf { ttt.value }
    val predictionColor by ColorConfig("Prediction Color", Color.ORANGE).showIf { ttt.value && prediction.value }

    val water by BooleanConfig("Enabled       ").section("Water Board Solver")
    val currentClickColor by ColorConfig("Click Color", Color.GREEN).showIf { water.value }
    val nextColor by ColorConfig("Next Click Color", Color.YELLOW).showIf { water.value }

    val icefill by BooleanConfig("Enabled        ").section("Ice Fill Solver")
    val icefillColor by ColorConfig("Click Color", Color.GREEN).showIf { icefill.value }

    val icepath by BooleanConfig("Enabled         ").section("Ice Path Solver")
    val icePathFirstColor by ColorConfig("Next Segment Color", Color.GREEN).showIf { icepath.value }
    val icePathColor by ColorConfig("Segments Color", Color.RED).showIf { icepath.value }

    override fun init() {
        val puzzles = PuzzleSolver::class.sealedSubclasses.mapNotNull { it.objectInstance }

        hudElement(
            name = "Quiz Timer",
            enabled = { quiz.value && quizTimer.value },
            shouldDraw = { QuizSolver.shouldShowTimer },
            centered = true
        ) { ctx, example ->
            val text = QuizSolver.timerText(example)
            ctx.drawCenteredString(text, 0f, 0f)
            return@hudElement text.width().toFloat() to 9f
        }.apply {
            scale = 3f
        }

        register<DungeonEvent.RoomEvent.onStateChange> { puzzles.forEach { if (it.enabled) it.onStateChange(event) } }
        register<PlayerInteractEvent.RIGHT_CLICK.BLOCK> { puzzles.forEach { if (it.enabled) it.onInteract(event) } }
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