package noammaddons.features.dungeons.solvers

import net.minecraft.block.Block
import net.minecraft.init.Blocks.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.dungeons.dmap.handlers.DungeonInfo
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.MathUtils.add
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ScanUtils.currentRoom
import noammaddons.utils.ScanUtils.getRealCoord
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.formatPbPuzzleMessage
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList


object CreeperBeamSolver: Feature() {
    private data class BeamPair(val start: BlockPos, val end: BlockPos, val color: Color)

    private var inCreeperBeams = false
    private var roomCenter = listOf(0, 0, 0)
    private var rotation = 0
    private var enterTimestamp: Long? = null
    private var solveTimestamp: Long? = null
    private val solutions = CopyOnWriteArrayList<BeamPair>()

    private val beamSolutions = listOf(
        Pair(listOf(0, 74, 0), listOf(0, 84, 2)),
        Pair(listOf(0, 78, 12), listOf(0, 76, - 12)),
        Pair(listOf(- 7, 80, - 7), listOf(11, 72, 7)),
        Pair(listOf(- 9, 77, 8), listOf(10, 76, - 9)),
        Pair(listOf(- 12, 78, 1), listOf(13, 75, - 1)),
        Pair(listOf(3, 78, 11), listOf(- 3, 76, - 12)),
        Pair(listOf(- 10, 79, - 6), listOf(11, 75, 6)),
        Pair(listOf(- 10, 76, - 8), listOf(9, 74, 10)),
        Pair(listOf(- 3, 82, 7), listOf(5, 69, - 12)),
        Pair(listOf(- 3, 81, - 6), listOf(6, 69, 12)),
        Pair(listOf(6, 81, - 5), listOf(- 11, 70, 8)),
        Pair(listOf(- 3, 81, - 6), listOf(6, 69, 12)),
        Pair(listOf(12, 76, - 3), listOf(11, 78, 3)),
        Pair(listOf(12, 76, - 3), listOf(- 11, 78, 3)),
        Pair(listOf(7, 80, 7), listOf(- 10, 72, - 8))
    )

    private val colorPool = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.ORANGE, Color.WHITE, Color.MAGENTA
    )

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! config.CreeperBeamSolver) return
        if (event.room.name != "Creeper Beams") return
        inCreeperBeams = true

        val (cx, cz) = getRoomCenterAt(mc.thePlayer.position)
        roomCenter = listOf(cx, 0, cz)
        rotation = DungeonInfo.uniqueRooms.find { it.name == currentRoom?.name }?.mainRoom?.rotation ?: return
        debugMessage("Creeper Beam detected - Rotation: $rotation, Center: $roomCenter")

        solve()
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) = reset()


    @SubscribeEvent
    fun renderSolutions(event: RenderWorld) {
        if (! inCreeperBeams) return

        solutions.forEach { (start, end, color) ->
            if (getBlockAt(start) != sea_lantern || getBlockAt(end) != sea_lantern) {
                if (solveTimestamp == null) solveTimestamp = System.currentTimeMillis()
                return@forEach
            }

            drawBlockBox(start, color, fill = true, outline = true, phase = config.CreeperBeamSolverPhase)
            drawBlockBox(end, color, fill = true, outline = true, phase = config.CreeperBeamSolverPhase)
            if (! config.CreeperBeamSolverLines) return@forEach
            draw3DLine(Vec3(start).add(0.5, 0.5, 0.5), Vec3(end).add(0.5, 0.5, 0.5), color, phase = config.CreeperBeamSolverPhase)
        }
    }

    @SubscribeEvent
    fun onPacketReceived(event: BlockChangeEvent) {
        if (! inCreeperBeams) return
        if (event.block != air) return
        if (event.pos == getRealCoord(listOf(- 1, 69, - 1), roomCenter, rotation)) return
        if (solutions.filter { getBlockAt(it.start) != sea_lantern && getBlockAt(it.end) != sea_lantern }.size < 4) return

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
            prefix = false
        )

        reset()
    }


    private fun solve() {
        var colorIndex = 0
        beamSolutions.forEach { beam ->
            if (colorIndex >= colorPool.size) return

            val startPos = getRealCoord(beam.first, roomCenter, rotation)
            val endPos = getRealCoord(beam.second, roomCenter, rotation)

            if (isBeamBlock(getBlockAt(startPos)) || isBeamBlock(getBlockAt(endPos))) return@forEach
            solutions.add(BeamPair(startPos, endPos, colorPool[colorIndex ++]))
        }

        enterTimestamp = System.currentTimeMillis()
    }

    private fun isBeamBlock(block: Block) = block.equalsOneOf(prismarine, sea_lantern)

    private fun reset() {
        if (! inCreeperBeams) return
        inCreeperBeams = false
        solutions.clear()
        enterTimestamp = null
        solveTimestamp = null
        roomCenter = emptyList()
        rotation = 0

        debugMessage("Creeper Beam Solver reset.")
    }
}

/*
private val beamSolutions = listOf(
        Pair(listOf(0, 74, 0), listOf(- 2, 84, 0)),
        Pair(listOf(- 12, 78, 0), listOf(12, 76, 0)),
        Pair(listOf(7, 80, - 7), listOf(- 7, 72, 11)),
        Pair(listOf(- 8, 77, - 9), listOf(9, 76, 10)),
        Pair(listOf(- 1, 78, - 12), listOf(1, 75, 13)),
        Pair(listOf(- 11, 78, 3), listOf(12, 76, - 3)),
        Pair(listOf(6, 79, - 10), listOf(- 6, 75, 11)),
        Pair(listOf(8, 76, - 10), listOf(- 10, 74, 9)),
        Pair(listOf(- 7, 82, - 3), listOf(12, 69, 5)),
        Pair(listOf(6, 81, - 3), listOf(- 12, 69, 6)),
        Pair(listOf(5, 81, 6), listOf(- 8, 70, - 11)),
        Pair(listOf(6, 81, - 3), listOf(- 12, 69, 6)),
        Pair(listOf(3, 76, 12), listOf(- 3, 78, 11)),
        Pair(rotateCoords(listOf(- 3, 76, - 12), 180), rotateCoords(listOf(3, 78, 11), 180)),
        Pair(rotateCoords(listOf(7, 80, - 7), 180), rotateCoords(listOf(- 8, 72, 10), 180))
    )
 */