package noammaddons.features.dungeons.solvers

import gg.essential.elementa.utils.withAlpha
import net.minecraft.init.Blocks.*
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
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
import noammaddons.utils.JsonUtils
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.ScanUtils.getRealCoord
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.ScanUtils.getRotation
import noammaddons.utils.Utils.formatPbPuzzleMessage


object BoulderSolver: Feature() {
    private data class BoulderBox(val box: BlockPos, val click: BlockPos, val render: BlockPos)

    private val boulderSolutions: MutableMap<String, List<List<Double>>> = mutableMapOf()
    private var currentSolution = mutableListOf<BoulderBox>()

    private var inBoulder = false
    private var roomCenter = emptyList<Int>()
    private var rotation = 0

    private val topLeftBox = BlockPos(- 9, 65, - 9)

    private val blockMapping = mapOf(
        chest to listOf(0, 66, - 14),
        iron_bars to listOf(0, 70, - 12),
    )

    var trueStartTime: Long? = null
    var startTime: Long? = null

    init {
        JsonUtils.fetchJsonWithRetry<Map<String, List<List<Double>>>?>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/BoulderSolutions.json"
        ) {
            it ?: return@fetchJsonWithRetry
            boulderSolutions.putAll(it)
        }
    }

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! config.boulderSolver) return
        if (event.room.name != "Boulder") return

        val center = getRoomCenterAt(mc.thePlayer.position)
        val detectedRotation = getRotation(center, blockMapping) ?: return

        inBoulder = true
        roomCenter = listOf(center.first, 0, center.second)
        rotation = detectedRotation * 90
        debugMessage("Boulder detected - Rotation: $rotation, Center: $roomCenter")

        solve()
        trueStartTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) = reset()

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = reset()

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! inBoulder) return
        if (currentSolution.isEmpty()) return

        if (config.boulderSolverShowAll) currentSolution.forEach { renderBox(it) }
        else renderBox(currentSolution.first())
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PacketEvent.Sent) {
        if (! inBoulder) return
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return
        val block = getBlockAt(packet.position)

        when (block) {
            wall_sign, stone_button -> {
                val removed = currentSolution.removeIf { it.click == packet.position }
                if (removed && startTime == null) startTime = System.currentTimeMillis()
            }

            chest -> {
                val enterTime = trueStartTime ?: return
                val solveTime = startTime ?: return

                val personalBestsData = personalBests.getData().pazzles
                val previousBest = personalBestsData["Boulder"]
                val completionTime = (System.currentTimeMillis() - solveTime).toDouble()
                val totalTime = (System.currentTimeMillis() - enterTime).toDouble()

                val message = formatPbPuzzleMessage("Boulder", completionTime, previousBest)

                sendPartyMessage(message)

                clickableChat(
                    msg = message,
                    cmd = "/na copy ${message.removeFormatting()}",
                    hover = "Total Time: &b${(totalTime / 1000.0).toFixed(2)}s",
                    prefix = false
                )

                reset()
            }
        }
    }

    private fun renderBox(box: BoulderBox) {
        drawBox(
            box.box.add(- 1, - 1, - 1).toVec(),
            box.box.add(2, 2, 2).toVec(),
            config.boulderSolverBoxColor.withAlpha(50),
            fill = true,
            outline = true,
            phase = true
        )

        drawBlockBox(
            box.click,
            config.boulderSolverClickColor,
            LineThickness = 2f,
            fill = true,
            outline = true,
            phase = true
        )

        /*
        drawBlockBox(
            box.click,
            Color.RED.withAlpha(70),
            LineThickness = 2f,
            fill = true,
            outline = true,
            phase = true
        )
        */
    }

    private fun solve() {
        val (sx, sy, sz) = topLeftBox.destructured()
        var str = ""

        for (z in 0 .. 5) {
            for (x in 0 .. 6) {
                val pos = getRealCoord(listOf(sx + x * 3, sy, sz + z * 3), roomCenter, rotation)
                val block = getBlockAt(pos)
                str += if (block == air) "0" else "1"
            }
        }

        debugMessage(str)

        currentSolution = boulderSolutions[str]?.map { sol ->
            val box = getRealCoord(listOf(sol[0].toInt(), sy, sol[1].toInt()), roomCenter, rotation)
            val click = getRealCoord(listOf(sol[2].toInt(), sy, sol[3].toInt()), roomCenter, rotation)
            val render = getRealCoord(listOf(sol[4].toInt(), sy, sol[5].toInt()), roomCenter, rotation)
            BoulderBox(box, click, render)
        }?.toMutableList() ?: return
    }

    private fun reset() {
        inBoulder = false
        roomCenter = emptyList()
        rotation = 0
        currentSolution.clear()
        trueStartTime = null
        startTime = null
    }
}
