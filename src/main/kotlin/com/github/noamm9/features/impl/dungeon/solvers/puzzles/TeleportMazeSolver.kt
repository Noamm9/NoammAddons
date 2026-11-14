package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.correctTpPadColor
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.tpMazeTracer
import com.github.noamm9.features.impl.dungeon.solvers.PuzzleSolvers.wrongTpPadColor
import com.github.noamm9.utils.MathUtils.center
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import com.github.noamm9.utils.render.world.Render3D.renderTracer
import com.github.noamm9.utils.render.world.RenderContext
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import kotlin.math.*

object TeleportMazeSolver: PuzzleSolver {
    override val enabled: Boolean get() = PuzzleSolvers.tpmaze.value

    private var minX: Int? = null
    private var minZ: Int? = null
    private var cells: List<Cell>? = null
    private var correctPad: TpPad? = null
    private var tracerPad: TpPad? = null
    private var inTpMaze = false

    override fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.name != "Teleport Maze") return

        val rotation = 360 - event.room.rotation !!
        val center = event.room.centerPos
        val pos1 = ScanUtils.getRealCoord(BlockPos(0, 69, - 3), center, rotation)
        if (WorldUtils.getBlockAt(pos1) != Blocks.END_PORTAL_FRAME) return

        inTpMaze = true

        scan(center)
        val player = mc.player ?: return
        updateTracer(player.x, player.z, player.yRot)
    }

    override fun onRenderWorld(ctx: RenderContext) {
        if (! inTpMaze) return
        val c = cells ?: return
        correctPad?.let { ctx.renderBlock(it.pos, correctTpPadColor.value, phase = true) }

        for (cell in c) for (pad in cell.pads) {
            if (! pad.blacklisted || isPadInStartOrEndCell(pad)) continue
            ctx.renderBlock(pad.pos, wrongTpPadColor.value)
        }

        if (! tpMazeTracer.value) return
        val nextPad = tracerPad ?: return
        val color = if (nextPad == correctPad) correctTpPadColor.value else Color.WHITE
        ctx.renderTracer(nextPad.pos.center.add(0.0, 5.0 / 16.0, 0.0), color)
    }

    override fun onPacket(event: MainThreadPacketReceivedEvent.Pre) {
        if (! inTpMaze) return
        val packet = event.packet as? ClientboundPlayerPositionPacket ?: return
        val pos = packet.change.position
        if (pos.x % 0.5 != 0.0 || pos.y != 69.5 || pos.z % 0.5 != 0.0) return
        val oldPad = getPadNear(mc.player !!.x, mc.player !!.z) ?: return
        val newPad = getPadNear(pos.x, pos.z) ?: return
        newPad.blacklisted = true
        oldPad.blacklisted = true
        correctPad = correctPad?.takeUnless { it.blacklisted }
        if (! isPadInStartOrEndCell(newPad)) {
            calcPadAngles(pos.x, pos.z, packet.change().yRot)
        }
        updateTracer(pos.x, pos.z, packet.change().yRot)
    }

    override fun reset() {
        inTpMaze = false
        minX = null
        minZ = null
        cells = null
        correctPad = null
        tracerPad = null
    }

    private fun scan(center: BlockPos) {
        val pads = mutableListOf<TpPad>()
        for (dx in 0 .. 31) for (dz in 0 .. 31) {
            val pos = BlockPos(center.x + dx - 16, 69, center.z + dz - 16)
            if (WorldUtils.getBlockAt(pos) != Blocks.END_PORTAL_FRAME) continue
            pads += TpPad(pos)
        }

        if (pads.isEmpty()) return

        minX = pads.minOf { it.pos.x }
        minZ = pads.minOf { it.pos.z }
        cells = List(9) { i -> Cell(i / 3, i % 3) }

        val mX = minX !!
        val mZ = minZ !!

        for (pad in pads) {
            pad.cellX = (pad.pos.x - mX) / 8
            pad.cellZ = (pad.pos.z - mZ) / 8

            val index = (pad.cellX * 3 + pad.cellZ).coerceIn(0, 8)
            cells !![index].pads += pad
        }
    }

    private fun updateTracer(px: Double, pz: Double, yaw: Float) {
        tracerPad = getCellAt(px, pz)?.pads?.filterNot { it.blacklisted }?.minWithOrNull(
            compareBy<TpPad> { it != correctPad }
                .thenBy { getAngleDiff(it.pos.x + 0.5 - px, it.pos.z + 0.5 - pz, yaw) }
                .thenBy { it.pos.center.distanceToSqr(px, 69.5, pz) }
        )
    }

    private fun calcPadAngles(px: Double, pz: Double, yaw: Float) {
        val currentPads = mutableListOf<TpPad>()
        val c = cells ?: return

        for (cell in c) for (pad in cell.pads) {
            if (isPadInStartOrEndCell(pad) || pad.blacklisted) continue

            val dx = pad.pos.x + 0.5 - px
            val dz = pad.pos.z + 0.5 - pz
            val angle = getAngleDiff(dx, dz, yaw)

            pad.totalAngle += angle
            currentPads.add(pad)
        }

        currentPads.sortBy { it.totalAngle }
        correctPad = currentPads.takeIf { it.size >= 2 && it[0].totalAngle != it[1].totalAngle }?.first()
    }

    private fun getAngleDiff(dx: Double, dz: Double, yaw: Float): Double {
        val yawRad = Math.toRadians(yaw.toDouble())
        val lookX = - sin(yawRad)
        val lookZ = cos(yawRad)

        val dist = sqrt(dx * dx + dz * dz)
        val targetX = dx / dist
        val targetZ = dz / dist

        val dot = (lookX * targetX + lookZ * targetZ).coerceIn(- 1.0, 1.0)
        val angle = Math.toDegrees(acos(dot))

        return angle
    }

    private fun getCellAt(x: Double, z: Double): Cell? {
        val mX = minX ?: return null
        val mZ = minZ ?: return null

        val xi = floor(x).toInt()
        val zi = floor(z).toInt()

        if (xi < mX || xi > mX + 23 || zi < mZ || zi > mZ + 23) return null

        val cellX = (xi - mX) / 8
        val cellZ = (zi - mZ) / 8

        return cells?.find { it.xIndex == cellX && it.zIndex == cellZ }
    }

    private fun getPadNear(x: Double, z: Double): TpPad? {
        val cell = getCellAt(x, z) ?: return null
        return cell.pads.find {
            (abs(x - (it.pos.x + 0.5)) + abs(z - (it.pos.z + 0.5))) <= 3.0
        }
    }

    private fun isPadInStartOrEndCell(pad: TpPad): Boolean {
        val c = cells ?: return false
        if (c.getOrNull(4)?.pads?.contains(pad) == true) return true

        for (cell in c) {
            if (cell != c[4] && cell.pads.size == 1 && pad in cell.pads) {
                return true
            }
        }
        return false
    }

    private class Cell(val xIndex: Int, val zIndex: Int) {
        val pads = mutableSetOf<TpPad>()
    }

    private class TpPad(
        val pos: BlockPos,
        var cellX: Int = 0,
        var cellZ: Int = 0,
        var totalAngle: Double = 0.0,
        var blacklisted: Boolean = false
    )
}