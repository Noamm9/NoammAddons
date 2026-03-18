package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.answerColor
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.quizTimer
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.startsWithOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos

object QuizSolver {
    private data class TriviaAnswer(var blockPos: BlockPos, var isCorrect: Boolean)

    private val quizSolutions by lazy {
        DataDownloader.loadJson<Map<String, List<String>>>("quizSolutions.json")
    }

    private val triviaOptions = List(3) { TriviaAnswer(BlockPos.ZERO, false) }

    private var triviaAnswers: List<String>? = null
    private var correctAnswer: String? = null

    private var inQuiz = false
    private var questionsStarted = false
    private var answerTime: Long = 0
    private var stage = 0

    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Quiz") return
        if (inQuiz) return

        inQuiz = true
        val center = event.room.centerPos
        val rotation = 360 - event.room.rotation !!

        triviaOptions[0].blockPos = ScanUtils.getRealCoord(BlockPos(5, 70, - 9), center, rotation)
        triviaOptions[1].blockPos = ScanUtils.getRealCoord(BlockPos(0, 70, - 6), center, rotation)
        triviaOptions[2].blockPos = ScanUtils.getRealCoord(BlockPos(- 5, 70, - 9), center, rotation)
    }

    fun onChat(event: ChatMessageEvent) {
        if (! LocationUtils.inDungeon) return
        val message = event.unformattedText
        val trimmed = message.trim()

        if (message.contains("I am Oruo the Omniscient. I have lived many lives.")) {
            questionsStarted = true
            stage = 1
            answerTime = DungeonListener.currentTime + 220
            return
        }

        if (message.contains("2 questions left... Then you will have proven your worth to me!")) {
            stage = 2
            answerTime = DungeonListener.currentTime + 100
            triviaOptions.forEach { it.isCorrect = false }
            return
        }

        if (message.contains("One more question!")) {
            stage = 3
            answerTime = DungeonListener.currentTime + 100
            triviaOptions.forEach { it.isCorrect = false }
            return
        }

        if (message.startsWith("[STATUE] Oruo the Omniscient: ") && message.contains("answered the final question") && message.endsWith("correctly!")) {
            questionsStarted = false
            reset()
            return
        }

        if (trimmed.startsWithOneOf("ⓐ", "ⓑ", "ⓒ")) {
            val optionChar = trimmed[0]
            triviaAnswers?.firstOrNull { message.endsWith(it) }?.let { matchedAnswer ->
                correctAnswer = "$optionChar $matchedAnswer"

                when (optionChar) {
                    'ⓐ' -> triviaOptions[0].isCorrect = true
                    'ⓑ' -> triviaOptions[1].isCorrect = true
                    'ⓒ' -> triviaOptions[2].isCorrect = true
                }

                ThreadUtils.scheduledTaskServer(2) {
                    ChatUtils.modMessage("&dQuizSolver &f> &aCorrect answer is: &b${"$optionChar $matchedAnswer"}")
                }
            }
            return
        }

        val newAnswers = when {
            trimmed == "What SkyBlock year is it?" -> {
                val year = (((System.currentTimeMillis() / 1000) - 1560276000) / 446400).toInt() + 1
                listOf("Year $year")
            }

            else -> quizSolutions.entries.find { message.contains(it.key) }?.value
        }

        if (newAnswers != null) triviaAnswers = newAnswers
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! inQuiz || triviaAnswers == null) return
        triviaOptions.forEach { answer ->
            if (! answer.isCorrect) return@forEach
            Render3D.renderBlock(ctx, answer.blockPos, answerColor.value, phase = true)
        }
    }

    fun onRenderOverlay(ctx: GuiGraphics) {
        if (quizTimer.value && questionsStarted && ! LocationUtils.inBoss) {
            val ticksLeft = answerTime - DungeonListener.currentTime
            if (ticksLeft <= 0) return
            val secondsLeft = (ticksLeft / 20.0).toFixed(1)
            Render2D.drawCenteredString(
                ctx,
                "§dQuiz §7(§f$stage/3§7): §b${secondsLeft}s",
                mc.window.guiScaledWidth / 2f,
                mc.window.guiScaledHeight / 3f,
                scale = 3f
            )
        }
    }

    fun reset() {
        inQuiz = false
        triviaOptions.forEach { it.isCorrect = false }
        triviaAnswers = null
        correctAnswer = null
        answerTime = - 1
        stage = 0
    }
}