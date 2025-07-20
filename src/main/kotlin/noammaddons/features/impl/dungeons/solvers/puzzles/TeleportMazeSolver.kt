package noammaddons.features.impl.dungeons.solvers.puzzles

import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.*
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.correctTpPadColor
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.tpMaze
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.wrongTpPadColor
import noammaddons.features.impl.general.teleport.InstantTransmissionPredictor.Vector3
import noammaddons.features.impl.general.teleport.InstantTransmissionPredictor.Vector3.Companion.fromPitchYaw
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ScanUtils
import noammaddons.utils.ScanUtils.getRoomCenterAt
import kotlin.math.abs

object TeleportMazeSolver {
    private var minX: Int? = null
    private var minZ: Int? = null
    private var cells: List<Cell>? = null
    private var orderedPads: MutableList<TpPad>? = null
    private var inTpMaze = false

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! tpMaze.value) return

        val center = getRoomCenterAt(mc.thePlayer.position)
        val pos1 = ScanUtils.getRealCoord(BlockPos(0, 69, - 3), center, 360 - event.room.rotation !!)

        if (getBlockAt(pos1) != Blocks.end_portal_frame) {
            inTpMaze = false
            return
        }

        inTpMaze = true

        val pads = mutableListOf<TpPad>()
        for (dx in 0 .. 31) {
            for (dz in 0 .. 31) {
                val pos = BlockPos(center.x + dx - 16, 69, center.z + dz - 16)
                if (getBlockAt(pos) != Blocks.end_portal_frame) continue
                pads += TpPad(pos)
            }
        }

        minX = pads.minOfOrNull { it.pos.x }
        minZ = pads.minOfOrNull { it.pos.z }
        cells = List(9) { i -> Cell(i / 3, i % 3) }

        for (pad in pads) {
            pad.cellX = (pad.pos.x - minX !!) / 8
            pad.cellZ = (pad.pos.z - minZ !!) / 8
            val index = pad.cellX * 3 + pad.cellZ
            cells !![index].addPad(pad)
        }
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) {
        if (inTpMaze) reset()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        reset()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! tpMaze.value) return
        val c = cells ?: return

        val top = orderedPads?.takeIf { it.size >= 2 }?.take(2) ?: return
        if (top[0].totalAngle == top[1].totalAngle) return

        drawBlockBox(top[0].pos, correctTpPadColor.value, outline = true, fill = true)

        c.forEach { cell ->
            cell.pads.filter { it.blacklisted }.forEach { pad ->
                drawBlockBox(pad.pos, wrongTpPadColor.value, outline = true, fill = true, phase = false)
            }
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! tpMaze.value) return
        if (cells == null) return
        val packet = event.packet as? S08PacketPlayerPosLook ?: return
        if (packet.x % 0.5 != 0.0 || packet.y != 69.5 || packet.z % 0.5 != 0.0) return
        val oldPad = getPadNear(mc.thePlayer.posX, mc.thePlayer.posZ) ?: return
        val newPad = getPadNear(packet.x, packet.z) ?: return

        if (isPadInStartOrEndCell(newPad)) return cells !!.forEach { cell ->
            cell.pads.forEach {
                it.blacklisted = false
                it.totalAngle = 0.0
            }
        }

        newPad.blacklisted = true
        oldPad.blacklisted = true
        newPad.twin = oldPad
        oldPad.twin = newPad

        calcPadAngles(packet.x, packet.z, packet.yaw)
    }

    private fun calcPadAngles(x: Double, z: Double, yaw: Float) {
        orderedPads = mutableListOf()
        for (cell in cells ?: return) {
            for (pad in cell.pads) {
                if (isPadInStartOrEndCell(pad) || pad.blacklisted) continue
                val padVec = Vector3(pad.pos.x + 0.5 - x, 0.0, pad.pos.z + 0.5 - z)
                pad.totalAngle += fromPitchYaw(.0, yaw.toDouble()).getAngleDeg(padVec)
                orderedPads?.add(pad)
            }
        }
        orderedPads?.sortBy { it.totalAngle }
    }

    private fun reset() {
        minX = null
        minZ = null
        cells = null
        orderedPads = null
        inTpMaze = false
    }

    private fun getCellAt(x: Int, z: Int): Cell? {
        val minX = minX ?: return null
        val minZ = minZ ?: return null
        if (x < minX || x > minX + 23 || z < minZ || z > minZ + 23) return null
        val cx = (x - minX) / 8
        val cz = (z - minZ) / 8
        return cells?.find { it.xIndex == cx && it.zIndex == cz }
    }

    private fun getPadNear(x: Double, z: Double): TpPad? {
        val cell = getCellAt(x.toInt(), z.toInt()) ?: return null
        return cell.pads.find {
            manhattanDistance(x, z, it.pos.x.toDouble(), it.pos.z.toDouble()) <= 3.0
        }
    }

    private fun manhattanDistance(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        return abs(x1 - x2) + abs(z1 - z2)
    }

    private fun isPadInStartOrEndCell(pad: TpPad): Boolean {
        val c = cells ?: return false
        if (c.getOrNull(4)?.pads?.contains(pad) == true) return true
        for (cell in c) {
            if (cell != c[4] && cell.pads.size == 1 && pad in cell.pads) return true
        }
        return false
    }

    private class Cell(val xIndex: Int, val zIndex: Int) {
        val pads = mutableSetOf<TpPad>()
        fun addPad(pad: TpPad) {
            pads += pad
        }
    }

    private class TpPad(
        val pos: BlockPos,
        var twin: TpPad? = null,
        var cellX: Int = 0,
        var cellZ: Int = 0,
        var totalAngle: Double = 0.0,
        var blacklisted: Boolean = false
    )
}