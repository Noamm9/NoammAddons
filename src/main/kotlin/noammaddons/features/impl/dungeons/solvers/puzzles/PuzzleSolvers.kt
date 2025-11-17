package noammaddons.features.impl.dungeons.solvers.puzzles

import gg.essential.elementa.utils.withAlpha
import net.minecraftforge.common.MinecraftForge
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.Utils.favoriteColor
import java.awt.Color

object PuzzleSolvers: Feature("Puzzle Solvers for Dungeon Clear") {
    private val solvers = listOf(
        CreeperBeamSolver, BlazeSolver,
        BoulderSolver, ThreeWeirdosSolver,
        TeleportMazeSolver, WaterBoardSolver,
        TicTacToeSolver, QuizSolver,
    )

    override fun onEnable() {
        super.onEnable()
        solvers.forEach(MinecraftForge.EVENT_BUS::register)
    }

    override fun onDisable() {
        super.onDisable()
        solvers.forEach(MinecraftForge.EVENT_BUS::unregister)
    }

    val creeper = ToggleSetting("Creeper Beam ")
    val CBlines = ToggleSetting("Draw Lines").addDependency(creeper)
    val CBphase = ToggleSetting("Phase", true).addDependency(creeper)

    val blaze = ToggleSetting("Blaze ")
    val blazeCount = SliderSetting("Blaze count", 1, 3, 1, 2).addDependency(blaze)
    val BlineColor = ColorSetting("Line Color", Color.WHITE, false).addDependency(blaze)
    val firstBlazeColor = ColorSetting("First Blaze Color", Color.GREEN, false).addDependency(blaze)
    val secondBlazeColor = ColorSetting("Second Blaze Color", Color.YELLOW, false).addDependency { blazeCount.value != 1 }.addDependency(blaze)
    val thirdBlazeColor = ColorSetting("Last Blaze Color", Color.RED, false).addDependency { blazeCount.value == 3 }.addDependency(blaze)

    val boulder = ToggleSetting("Boulder ")
    val BshowAll = ToggleSetting("Show All", false).addDependency(boulder)

    //val BzeroPing = ToggleSetting("Zero Ping", false).addDependency(boulder) - R.I.P goodbye stonking
    val BboxColor = ColorSetting("Box Color", favoriteColor.withAlpha(40)).addDependency(boulder)
    val BclickColor = ColorSetting("Click Color", Color.RED.withAlpha(0.3f)).addDependency(boulder)

    val weirdos = ToggleSetting("Three Weirdos ")
    val WremoveNPCS = ToggleSetting("Remove NPCS", false).addDependency(weirdos)
    val WremoveChests = ToggleSetting("Remove Wrong Chests", false).addDependency(weirdos)
    val Wcolor = ColorSetting("Correct Chest Color", favoriteColor.withAlpha(40)).addDependency(weirdos)
    val WcolorWrong = ColorSetting("Incorrect Chest Color", Color.RED.withAlpha(40)).addDependency { WremoveChests.value }.addDependency(weirdos)

    val tpMaze = ToggleSetting("Teleport Maze ")
    val correctTpPadColor = ColorSetting("Correct Pad Color", Color.GREEN.withAlpha(80)).addDependency(tpMaze)
    val wrongTpPadColor = ColorSetting("Wrong Pad Color", Color.RED.withAlpha(111)).addDependency(tpMaze)

    val waterBoard = ToggleSetting("Water Board ")
    val firstTracerColor = ColorSetting("First Tracer Color", favoriteColor, false).addDependency(waterBoard)
    val secondTracerColor = ColorSetting("Second Tracer Color", Color.YELLOW, false).addDependency(waterBoard)

    val ticTacToe = ToggleSetting("Tic Tac Toe ")
    val ticTacToeColor = ColorSetting("Solver Color", favoriteColor.withAlpha(50)).addDependency(ticTacToe)

    val quiz = ToggleSetting("Quiz ")
    val quizColor = ColorSetting("Solver Color", favoriteColor.withAlpha(40)).addDependency(quiz)

    override fun init() = addSettings(
        SeperatorSetting("Creeper Beam"),
        creeper, CBlines, CBphase,
        SeperatorSetting("Blaze"),
        blaze, blazeCount, BlineColor, firstBlazeColor, secondBlazeColor, thirdBlazeColor,
        SeperatorSetting("Boulder"),
        boulder, BshowAll,/* BzeroPing, */BboxColor, BclickColor,
        SeperatorSetting("Three Weirdos"),
        weirdos, WremoveNPCS, WremoveChests, Wcolor, WcolorWrong,
        SeperatorSetting("Teleport Maze"),
        tpMaze, correctTpPadColor, wrongTpPadColor,
        SeperatorSetting("Water Board"),
        waterBoard, firstTracerColor, secondTracerColor,
        SeperatorSetting("Tic Tac Toe"),
        ticTacToe, ticTacToeColor,
        SeperatorSetting("Quiz"),
        quiz, quizColor
    )
}