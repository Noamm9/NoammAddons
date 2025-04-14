package noammaddons.features.dungeons.solvers

import net.minecraft.init.Blocks.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ScanUtils.getRealCoord
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.ScanUtils.getRotation
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
        Pair(listOf(3, 76, 12), listOf(- 3, 78, 11))
    )

    private val colorPool = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.ORANGE, Color.WHITE, Color.MAGENTA
    )

    private val blockMapping = mapOf(
        leaves to listOf(3, 80, 2),
        dirt to listOf(4, 68, 3),
        cobblestone to listOf(- 4, 68, - 3)
    )

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! config.CreeperBeamSolver) return
        if (event.room.name != "Creeper Beams") return

        val center = getRoomCenterAt(mc.thePlayer.position)
        val detectedRotation = getRotation(center, blockMapping) ?: return

        inCreeperBeams = true
        roomCenter = listOf(center.first, 0, center.second)
        rotation = detectedRotation * 90
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
            draw3DLine(start.toVec().add(Vec3(0.5, 0.5, 0.5)), end.toVec().add(Vec3(0.5, 0.5, 0.5)), color)
        }
    }

    @SubscribeEvent
    fun onPacketReceived(event: BlockChangeEvent) {
        if (! inCreeperBeams) return
        if (event.block != air) return
        if (event.pos == getRealCoord(listOf(- 1, 69, - 1), roomCenter, rotation)) return
        if (solutions.filter { getBlockAt(it.start) != sea_lantern || getBlockAt(it.end) != sea_lantern }.size < 4) return

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

            if (getBlockAt(startPos) != sea_lantern || getBlockAt(endPos) != sea_lantern) return@forEach
            solutions.add(BeamPair(startPos, endPos, colorPool[colorIndex ++]))
        }

        enterTimestamp = System.currentTimeMillis()
    }

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
