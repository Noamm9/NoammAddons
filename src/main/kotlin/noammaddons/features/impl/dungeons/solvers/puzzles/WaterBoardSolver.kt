package noammaddons.features.impl.dungeons.solvers.puzzles

import kotlinx.serialization.json.*
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
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toPos
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.ScanUtils.getRealCoord
import noammaddons.utils.ScanUtils.getRoomCenter
import noammaddons.utils.ScanUtils.getRoomCorner
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.formatPbPuzzleMessage
import noammaddons.utils.WebUtils

object WaterBoardSolver {
    private lateinit var waterSolutions: JsonObject

    init {
        WebUtils.get("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/waterSolutions.json") {
            waterSolutions = it
        }
    }

    private var solutions = HashMap<LeverBlock, Array<Double>>()
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
        if (event.room.rotation == null) return modMessage("Failed to get WaterBoard room Rotation")
        if (patternIdentifier != - 1) return

        roomCenter = getRoomCenter(getRoomCorner(event.room.getRoomComponent())).run { BlockPos(first, 0, second) }
        rotation = 360 - event.room.rotation !!
        trueStartTime = System.currentTimeMillis()

        setTimeout(1000) {
            debugMessage("started scanning water")

            val closeWalls = WoolColor.entries.joinToString("") {
                if (it.isClose) it.ordinal.toString() else ""
            }.takeIf { it.length == 3 } ?: return@setTimeout

            debugMessage("started scanning patternIdentifier")

            patternIdentifier = when {
                getBlockAt(getRealCoord(BlockPos(- 1, 77, 12), roomCenter !!, rotation !!)) == Blocks.hardened_clay -> 0
                getBlockAt(getRealCoord(BlockPos(1, 78, 12), roomCenter !!, rotation !!)) == Blocks.emerald_block -> 1
                getBlockAt(getRealCoord(BlockPos(- 1, 78, 12), roomCenter !!, rotation !!)) == Blocks.diamond_block -> 2
                getBlockAt(getRealCoord(BlockPos(- 1, 78, 12), roomCenter !!, rotation !!)) == Blocks.quartz_block -> 3
                else -> return@setTimeout modMessage("&cFailed to get Water Board pattern. Was the puzzle already started?")
            }

            debugMessage("water: patternIdentifier: $patternIdentifier")

            solutions.clear()
            waterSolutions[patternIdentifier.toString()]?.jsonObject?.get(closeWalls)?.jsonObject?.entries?.forEach { entry ->
                solutions[
                    when (entry.key) {
                        "diamond_block" -> LeverBlock.DIAMOND
                        "emerald_block" -> LeverBlock.EMERALD
                        "hardened_clay" -> LeverBlock.CLAY
                        "quartz_block" -> LeverBlock.QUARTZ
                        "gold_block" -> LeverBlock.GOLD
                        "coal_block" -> LeverBlock.COAL
                        "water" -> LeverBlock.WATER
                        else -> LeverBlock.NONE
                    }
                ] = entry.value.jsonArray.map { it.jsonPrimitive.double }.toTypedArray()
            }
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
        drawTracer(firstSolution.leverPos.addVector(.5, .5, .5), color = firstTracerColor.value, lineWidth = 1.5f)

        if (solutionList.size > 1 && firstSolution.leverPos != solutionList[1].first.leverPos) {
            draw3DLine(
                firstSolution.leverPos.addVector(.5, .5, .5), solutionList[1].first.leverPos.addVector(.5, .5, .5),
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
                    lever.leverPos.addVector(0.5, (index + lever.i) * 0.5 + 1.5, 0.5), scale = 1.35f, phase = true
                )
            }
        }
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) = with(event.packet) {
        if (solutions.isEmpty()) return@with
        if (this !is C08PacketPlayerBlockPlacement) return@with

        LeverBlock.entries.find { it.leverPos.toPos() == position }?.let {
            if (startTime == null) startTime = System.currentTimeMillis()
            if (it == LeverBlock.WATER && openedWaterTicks == - 1) openedWaterTicks = tickCounter
            it.i ++
        }

        if (getBlockAt(position) == Blocks.chest) {
            if (WoolColor.entries.any { it.isClose }) return@with

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

        inline val isClose: Boolean
            get() = getBlockAt(getRealCoord(relativePosition, roomCenter !!, rotation !!)) == Blocks.wool
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

        inline val leverPos: Vec3
            get() {
                val c = roomCenter ?: return Vec3(0.0, 0.0, 0.0)
                val r = rotation ?: return Vec3(0.0, 0.0, 0.0)
                return getRealCoord(relativePosition.toPos(), c, r).toVec()
            }
    }
}