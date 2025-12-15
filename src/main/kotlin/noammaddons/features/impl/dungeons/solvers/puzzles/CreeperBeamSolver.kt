package noammaddons.features.impl.dungeons.solvers.puzzles

import net.minecraft.block.Block
import net.minecraft.init.Blocks.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.events.*
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.CBlines
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.CBphase
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.DataDownloader
import noammaddons.utils.MathUtils.center
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ScanUtils
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.formatPbPuzzleMessage
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList


object CreeperBeamSolver {
    private data class BeamPair(val start: BlockPos, val end: BlockPos, val color: Color = Color.WHITE)

    private val beamSolutions = DataDownloader.loadJson<List<List<List<Int>>>>("CreeperBeamSolutions.json").map {
        BeamPair(BlockPos(it[0][0], it[0][1], it[0][2]), BlockPos(it[1][0], it[1][1], it[1][2]))
    }

    private val currentSolve = CopyOnWriteArrayList<BeamPair>()

    private var inCreeperBeams = false
    private var roomCenter = BlockPos(- 1, - 1, - 1)
    private var rotation = 0

    private var enterTimestamp: Long? = null
    private var solveTimestamp: Long? = null

    private val colorPool = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.ORANGE, Color.WHITE, Color.MAGENTA
    )


    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! PuzzleSolvers.creeper.value) return
        if (event.room.name != "Creeper Beams") return
        inCreeperBeams = true

        // I accidentally took all solutions coords with a 180 rotation room
        rotation = 360 - event.room.rotation !! + 180
        roomCenter = ScanUtils.getRoomCenter(event.room.mainRoom)

        solve()
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) = reset()

    @SubscribeEvent
    fun renderSolutions(event: RenderWorld) {
        if (! inCreeperBeams) return

        currentSolve.forEach { (start, end, color) ->
            if (getBlockAt(start) != sea_lantern || getBlockAt(end) != sea_lantern) {
                if (solveTimestamp == null) solveTimestamp = System.currentTimeMillis()
                return@forEach
            }

            drawBlockBox(start, color, fill = true, outline = true, phase = CBphase.value)
            drawBlockBox(end, color, fill = true, outline = true, phase = CBphase.value)

            if (CBlines.value) {
                val pos1 = Vec3(start).center()
                val pos2 = Vec3(end).center()
                draw3DLine(pos1, pos2, color, phase = CBphase.value)
            }
        }
    }

    @SubscribeEvent
    fun onPacketReceived(event: BlockChangeEvent) {
        if (! inCreeperBeams) return
        if (event.block != air) return
        if (event.pos == ScanUtils.getRealCoord(BlockPos(- 1, 69, - 1), roomCenter, rotation)) return
        if (currentSolve.filter { getBlockAt(it.start) != sea_lantern && getBlockAt(it.end) != sea_lantern }.size < 4) return

        val enterTime = enterTimestamp ?: return
        val solveTime = solveTimestamp ?: return

        val personalBestsData = personalBests.getData().pazzles
        val previousBest = personalBestsData["Creeper Beams"]
        val completionTime = (System.currentTimeMillis() - solveTime).toDouble()
        val totalTime = (System.currentTimeMillis() - enterTime).toDouble()

        val message = formatPbPuzzleMessage("Creeper Beams", completionTime, previousBest)

        sendPartyMessage(message)

        clickableChat(
            msg = message,
            cmd = "/na copy ${message.removeFormatting()}",
            hover = "Total Time: &b${(totalTime / 1000.0).toFixed(2)}s",
        )

        reset()
    }


    private fun solve() {
        var colorIndex = 0
        beamSolutions.forEach { beam ->
            if (colorIndex >= colorPool.size) return@forEach

            val startPos = ScanUtils.getRealCoord(beam.start, roomCenter, rotation)
            val endPos = ScanUtils.getRealCoord(beam.end, roomCenter, rotation)

            if (! isBeamBlock(getBlockAt(startPos)) || ! isBeamBlock(getBlockAt(endPos))) return@forEach
            currentSolve.add(BeamPair(startPos, endPos, colorPool[colorIndex ++]))
        }

        enterTimestamp = System.currentTimeMillis()
    }

    private fun isBeamBlock(block: Block) = block.equalsOneOf(prismarine, sea_lantern)

    private fun reset() {
        if (! inCreeperBeams) return
        inCreeperBeams = false
        currentSolve.clear()
        enterTimestamp = null
        solveTimestamp = null
        roomCenter = BlockPos(- 1, - 1, - 1)
        rotation = 0
    }
}