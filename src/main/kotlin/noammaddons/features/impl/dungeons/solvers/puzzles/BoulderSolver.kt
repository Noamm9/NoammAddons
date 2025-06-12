package noammaddons.features.impl.dungeons.solvers.puzzles

import net.minecraft.init.Blocks.*
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.BboxColor
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.BclickColor
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.BshowAll
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.BzeroPing
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.*
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toVec
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.ScanUtils.getRealCoord
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.Utils.formatPbPuzzleMessage


object BoulderSolver: Feature() {
    private data class BoulderBox(val box: BlockPos, val click: BlockPos, val render: BlockPos)

    private val boulderSolutions: MutableMap<String, List<List<Double>>> = mutableMapOf()
    private var currentSolution = mutableListOf<BoulderBox>()
    private val bottemLeftBox = BlockPos(- 9, 65, - 9)

    private var inBoulder = false
    private var roomCenter = BlockPos(- 1, - 1, - 1)
    private var rotation = 0

    var trueStartTime: Long? = null
    var startTime: Long? = null

    init {
        WebUtils.fetchJsonWithRetry<Map<String, List<List<Double>>>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/BoulderSolutions.json"
        ) {
            boulderSolutions.putAll(it)
        }
    }

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.data.name != "Boulder") return
        inBoulder = true

        // I accidentally took all solutions coords with a 180 rotation room
        rotation = 360 - event.room.rotation !! + 180
        roomCenter = getRoomCenterAt(mc.thePlayer.position)

        solve()
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) = reset()

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = reset()

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! inBoulder) return
        if (currentSolution.isEmpty()) return

        if (BshowAll.value) currentSolution.forEach { renderBox(it) }
        else renderBox(currentSolution.first())
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PacketEvent.Sent) {
        if (! inBoulder) return
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return
        val block = getBlockAt(packet.position)

        when (block) {
            wall_sign, stone_button -> {
                val entry = currentSolution.find { it.click == packet.position } ?: return
                if (currentSolution.remove(entry) && startTime == null) startTime = System.currentTimeMillis()
                if (! BzeroPing.value) return

                MathUtils.getAllBlocksBetween(entry.box.add(- 1, - 1, - 1), entry.box.add(1, 1, 1)).forEach {
                    BlockUtils.toAir(it)
                }
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
            BboxColor.value,
            fill = true,
            outline = true,
            phase = true
        )

        drawBlockBox(
            box.click,
            BclickColor.value,
            LineThickness = 2f,
            fill = true,
            outline = true,
            phase = true
        )
    }

    private fun solve() {
        val (sx, sy, sz) = bottemLeftBox.destructured()
        var str = ""

        for (z in 0 .. 5) {
            for (x in 0 .. 6) {
                val pos = getRealCoord(BlockPos(sx + x * 3, sy, sz + z * 3), roomCenter, rotation)
                str += if (getBlockAt(pos) == air) "0" else "1"
            }
        }

        debugMessage(str)

        currentSolution = boulderSolutions[str]?.map { sol ->
            val box = getRealCoord(BlockPos(sol[0].toInt(), sy, sol[1].toInt()), roomCenter, rotation)
            val click = getRealCoord(BlockPos(sol[2].toInt(), sy, sol[3].toInt()), roomCenter, rotation)
            val render = getRealCoord(BlockPos(sol[4].toInt(), sy, sol[5].toInt()), roomCenter, rotation)
            BoulderBox(box, click, render)
        }?.toMutableList() ?: return

        trueStartTime = System.currentTimeMillis()
    }

    private fun reset() {
        inBoulder = false
        roomCenter = BlockPos(- 1, - 1, - 1)
        rotation = 0
        currentSolution.clear()
        trueStartTime = null
        startTime = null
    }
}