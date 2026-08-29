package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers
import com.github.noamm9.init.DataDownloader
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import com.github.noamm9.utils.render.world.RenderContext
import com.github.noamm9.utils.startsWithOneOf
import net.minecraft.core.BlockPos

object QuizSolver: PuzzleSolver {
    override val enabled: Boolean get() = PuzzleSolvers.quiz.value

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

    val shouldShowTimer get() = questionsStarted && ! LocationUtils.inBoss && answerTime - DungeonListener.currentTime > 0
    fun timerText(example: Boolean): String {
        val displayStage = if (example) 1 else stage
        val secondsLeft = if (example) "10.0" else ((answerTime - DungeonListener.currentTime) / 20.0).toFixed(1)
        return "§dQuiz §7(§f$displayStage/3§7): §b${secondsLeft}s"
    }

    override fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Quiz") return
        if (inQuiz) return

        inQuiz = true
        val center = event.room.centerPos
        val rotation = 360 - event.room.rotation !!

        triviaOptions[0].blockPos = ScanUtils.getRealCoord(BlockPos(5, 70, - 9), center, rotation)
        triviaOptions[1].blockPos = ScanUtils.getRealCoord(BlockPos(0, 70, - 6), center, rotation)
        triviaOptions[2].blockPos = ScanUtils.getRealCoord(BlockPos(- 5, 70, - 9), center, rotation)
    }

    override fun onChat(event: ChatMessageEvent) {
        if (! LocationUtils.inDungeon) return
        val message = event.unformattedText
        val trimmed = message.trim()

        if (message.contains("I am Oruo the Omniscient. I have lived many lives.")) {
            triviaOptions.forEach { it.isCorrect = false }
            triviaAnswers = null
            correctAnswer = null
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

    override fun onInteract(event: PlayerInteractEvent.RIGHT_CLICK.BLOCK) {
        if (! inQuiz || ! PuzzleSolvers.quizBlockWrongClicks.value) return
        if (mc.player?.isCrouching == true) return

        val clickedAnswer = triviaOptions.firstOrNull { it.blockPos.distManhattan(event.pos) <= 1 } ?: return
        if (triviaOptions.none { it.isCorrect }) return
        if (! clickedAnswer.isCorrect) event.cancel()
    }

    override fun onRenderWorld(ctx: RenderContext) {
        if (! inQuiz || triviaAnswers == null) return
        triviaOptions.forEach { answer ->
            if (! answer.isCorrect) return@forEach
            ctx.renderBlock(answer.blockPos, PuzzleSolvers.answerColor.value, phase = true)
        }
    }

    override fun onRoomExit() {}

    override fun reset() {
        inQuiz = false
        triviaOptions.forEach { it.isCorrect = false }
        triviaAnswers = null
        correctAnswer = null
        answerTime = - 1
        stage = 0
    }
}