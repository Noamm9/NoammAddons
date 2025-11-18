package noammaddons.features.impl.dungeons.solvers.puzzles

import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.events.*
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.firstTracerColor
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.secondTracerColor
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.waterBoard
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toPos
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.ScanUtils.getRealCoord
import noammaddons.utils.Utils.formatPbPuzzleMessage

object WaterBoardSolver {
    private val waterSolutions = DataDownloader.loadJson<Map<String, Map<String, Map<String, List<Double>>>>>("waterSolutions.json")

    private var solutions = HashMap<LeverBlock, List<Double>>()
    private var patternIdentifier = - 1
    private var openedWaterTicks = - 1
    private var tickCounter = 0

    private var roomCenter: BlockPos? = null
    private var rotation: Int? = null

    private var trueStartTime: Long? = null
    private var startTime: Long? = null

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! waterBoard.value) return
        if (event.room.data.name != "Water Board") return
        if (patternIdentifier != - 1) return

        roomCenter = ScanUtils.getRoomCenter(event.room)
        rotation = 360 - event.room.rotation !!
        trueStartTime = System.currentTimeMillis()

        ThreadUtils.scheduledTask(20) {
            solve()
        }
    }

    private fun solve() {
        val roomCenter = roomCenter ?: return
        val rotation = rotation ?: return

        val closeWalls = WoolColor.entries.joinToString("") {
            if (it.isClose()) it.ordinal.toString() else ""
        }.takeIf { it.length == 3 } ?: return

        patternIdentifier = when {
            getBlockAt(getRealCoord(BlockPos(- 1, 77, 12), roomCenter, rotation)) == Blocks.hardened_clay -> 0
            getBlockAt(getRealCoord(BlockPos(1, 78, 12), roomCenter, rotation)) == Blocks.emerald_block -> 1
            getBlockAt(getRealCoord(BlockPos(- 1, 78, 12), roomCenter, rotation)) == Blocks.diamond_block -> 2
            getBlockAt(getRealCoord(BlockPos(- 1, 78, 12), roomCenter, rotation)) == Blocks.quartz_block -> 3
            else -> return modMessage("&cFailed to get Water Board pattern. Was the puzzle already started?")
        }

        solutions.clear()
        waterSolutions["$patternIdentifier"]?.get(closeWalls)?.entries?.forEach { entry ->
            solutions[LeverBlock.fromString(entry.key)] = entry.value
        }
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) {
        if (patternIdentifier == - 1) return
        reset()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (patternIdentifier == - 1 || solutions.isEmpty()) return

        val solutionList = solutions
            .flatMap { (lever, times) -> times.drop(lever.i).map { Pair(lever, it) } }
            .sortedBy { (lever, time) -> time + if (lever == LeverBlock.WATER) 0.01 else 0.0 }

        val firstSolution = solutionList.firstOrNull()?.first ?: return
        drawTracer(firstSolution.getLever().addVector(.5, .5, .5), color = firstTracerColor.value, lineWidth = 1.5f)

        if (solutionList.size > 1 && firstSolution.getLever() != solutionList[1].first.getLever()) {
            draw3DLine(
                firstSolution.getLever().addVector(.5, .5, .5), solutionList[1].first.getLever().addVector(.5, .5, .5),
                color = secondTracerColor.value, lineWidth = 1.5f, false
            )
        }

        solutions.forEach { (lever, times) ->
            times.drop(lever.i).forEachIndexed { index, time ->
                val timeInTicks = (time * 20).toInt()
                RenderUtils.drawString(
                    when {
                        openedWaterTicks == - 1 && timeInTicks == 0 -> "&a&lCLICK"
                        openedWaterTicks == - 1 -> {
                            if (time < 2) "&c${time}s"
                            else if (time < 6) "&e${time}s"
                            else "&a${time}s"
                        }

                        else -> {
                            val remainingTicks = openedWaterTicks + timeInTicks - tickCounter
                            if (remainingTicks > 0) {
                                val remainingSeconds = remainingTicks / 20.0

                                if (remainingSeconds < 2) "&c${remainingSeconds.toFixed(1)}s"
                                else if (remainingSeconds < 6) "&e${remainingSeconds.toFixed(1)}s"
                                else "&a${remainingSeconds.toFixed(1)}s"
                            }
                            else "&a&lCLICK"
                        }
                    },
                    lever.getLever().addVector(0.5, (index + lever.i) * 0.5 + 1.5, 0.5), scale = 1.35f, phase = true
                )
            }
        }
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        if (solutions.isEmpty()) return
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return

        LeverBlock.entries.find { it.getLever().toPos() == packet.position }?.let {
            if (startTime == null) startTime = System.currentTimeMillis()
            if (it == LeverBlock.WATER && openedWaterTicks == - 1) openedWaterTicks = tickCounter
            it.i ++
        }

        if (getBlockAt(packet.position) == Blocks.chest) {
            if (WoolColor.entries.any { it.isClose() }) return

            val personalBestsData = personalBests.getData().pazzles
            val previousBest = personalBestsData["Water Board"]
            val completionTime = (System.currentTimeMillis() - startTime !!).toDouble()
            val totalTime = (System.currentTimeMillis() - trueStartTime !!).toDouble()

            val message = formatPbPuzzleMessage("Water Board", completionTime, previousBest)

            sendPartyMessage(message)

            clickableChat(
                msg = message,
                cmd = "/na copy ${message.removeFormatting()}",
                hover = "Total Time: &b${(totalTime / 1000.0).toFixed(2)}s",
                prefix = false
            )

            reset()
            trueStartTime = null
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (patternIdentifier == - 1) return
        tickCounter ++
    }

    fun reset() {
        LeverBlock.entries.forEach { it.i = 0 }
        patternIdentifier = - 1
        solutions.clear()
        openedWaterTicks = - 1
        tickCounter = 0
        roomCenter = null
        rotation = null
        startTime = null
    }

    private enum class WoolColor(val relativePosition: BlockPos) {
        PURPLE(BlockPos(0, 56, 4)),
        ORANGE(BlockPos(0, 56, 3)),
        BLUE(BlockPos(0, 56, 2)),
        GREEN(BlockPos(0, 56, 1)),
        RED(BlockPos(0, 56, 0));

        fun isClose() = getBlockAt(getRealCoord(relativePosition, roomCenter !!, rotation !!)) == Blocks.wool
    }

    private enum class LeverBlock(val relativePosition: Vec3, var i: Int = 0) {
        QUARTZ(Vec3(5.0, 61.0, 5.0)),
        GOLD(Vec3(5.0, 61.0, 0.0)),
        COAL(Vec3(5.0, 61.0, - 5.0)),
        DIAMOND(Vec3(- 5.0, 61.0, 5.0)),
        EMERALD(Vec3(- 5.0, 61.0, 0.0)),
        CLAY(Vec3(- 5.0, 61.0, - 5.0)),
        WATER(Vec3(0.0, 60.0, - 10.0)),
        NONE(Vec3(0.0, 0.0, 0.0));

        fun getLever() = getRealCoord(relativePosition.toPos(), roomCenter !!, rotation !!).toVec()

        companion object {
            fun fromString(str: String) = when (str) {
                "diamond_block" -> DIAMOND
                "emerald_block" -> EMERALD
                "hardened_clay" -> CLAY
                "quartz_block" -> QUARTZ
                "gold_block" -> GOLD
                "coal_block" -> COAL
                "water" -> WATER
                else -> NONE
            }
        }
    }
}