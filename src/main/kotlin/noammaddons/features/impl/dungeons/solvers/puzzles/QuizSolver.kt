package noammaddons.features.impl.dungeons.solvers.puzzles

import gg.essential.elementa.utils.withAlpha
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.events.*
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.ScanUtils.getRealCoord
import noammaddons.utils.Utils.formatPbPuzzleMessage
import noammaddons.utils.Utils.startsWithOneOf
import java.awt.Color


/**
 * Contains modified code from Krypt's Quiz solver.
 *
 * Original File: [GitHub](https://github.com/StellariumMC/Krypt/blob/master/src/main/kotlin/xyz/meowing/krypt/features/solvers/QuizSolver.kt)
 */
object QuizSolver {
    private data class TriviaAnswer(var blockPos: BlockPos, var isCorrect: Boolean)

    private val quizSolutions = DataDownloader.loadJson<Map<String, List<String>>>("quizSolutions.json")
    private val triviaOptions = List(3) { TriviaAnswer(BlockPos(0, 0, 0), false) }
    private var triviaAnswers: List<String>? = null
    private var correctAnswer: String? = null

    private var inQuiz = false
    private var timeStarted: Long? = null
    private var ticks = - 1


    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        if (inQuiz) inQuiz = false
    }

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! PuzzleSolvers.quiz.value) return
        if (inQuiz || event.room.data.name != "Quiz") return

        inQuiz = true
        val roomCenter = ScanUtils.getRoomCenter(event.room)
        val rotation = 360 - event.room.rotation !!

        triviaOptions[0].blockPos = getRealCoord(BlockPos(5, 70, - 9), roomCenter, rotation)
        triviaOptions[1].blockPos = getRealCoord(BlockPos(0, 70, - 6), roomCenter, rotation)
        triviaOptions[2].blockPos = getRealCoord(BlockPos(- 5, 70, - 9), roomCenter, rotation)

        timeStarted = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! PuzzleSolvers.quiz.value) return
        if (! inDungeon || inBoss) return
        val message = event.component.noFormatText
        val trimmed = message.trim()

        if (message == "[STATUE] Oruo the Omniscient: I am Oruo the Omniscient. I have lived many lives. I have learned all there is to know.") {
            if (ticks == - 1) ticks = 0
        }

        when {
            message.startsWith("[STATUE] Oruo the Omniscient: ") && message.endsWith("correctly!") -> {
                if (message.contains("answered the final question")) {
                    triviaOptions.forEach {
                        it.blockPos = BlockPos(0, 0, 0)
                        it.isCorrect = false
                    }
                    triviaAnswers = null
                    ticks = 0
                    correctAnswer = null
                    timeStarted?.let {
                        val personalBestsData = personalBests.getData().pazzles
                        val previousBest = personalBestsData["Quiz"]
                        val completionTime = (System.currentTimeMillis() - it).toDouble()
                        val pbMessage = formatPbPuzzleMessage("Quiz", completionTime, previousBest)

                        sendPartyMessage(pbMessage)
                        clickableChat(msg = pbMessage, cmd = "/na copy ${pbMessage.removeFormatting()}", prefix = false)
                        timeStarted = null
                    }
                }
                else if (message.contains("answered Question #")) {
                    triviaOptions.forEach { it.isCorrect = false }
                }
            }

            trimmed.startsWithOneOf("ⓐ", "ⓑ", "ⓒ") -> {
                triviaAnswers?.firstOrNull { message.endsWith(it) }?.let {
                    correctAnswer = it
                    when (trimmed[0]) {
                        'ⓐ' -> triviaOptions[0].isCorrect = true
                        'ⓑ' -> triviaOptions[1].isCorrect = true
                        'ⓒ' -> triviaOptions[2].isCorrect = true
                    }
                }
            }

            else -> {
                val newAnswers = when {
                    trimmed == "What SkyBlock year is it?" -> {
                        val year = (((System.currentTimeMillis() / 1000) - 1560276000) / 446400).toInt() + 1
                        listOf("Year $year")
                    }

                    else -> quizSolutions.entries.find { message.contains(it.key) }?.value
                }
                newAnswers?.let { triviaAnswers = it }
            }
        }

        correctAnswer?.let {
            correctAnswer = null
            ThreadUtils.scheduledTask(2) {
                modMessage("&dQuizSolver &f> &aCorrect answer is: $it")
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! inQuiz || triviaAnswers == null || inBoss) return

        triviaOptions.toList().forEach { answer ->
            if (! answer.isCorrect) return@forEach
            val pos = answer.blockPos

            RenderUtils.drawBlockBox(pos, Color.CYAN.withAlpha(40), outline = true, fill = true)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (ticks > 0) {
            val timeleft = (242 - ticks) / 20
            if (timeleft <= 0) {
                ticks = - 1
                return
            }
            RenderUtils.drawTitle("", "&dQuiz: &b$timeleft")
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (inQuiz && ticks != - 1) {
            ticks ++
        }
    }
}