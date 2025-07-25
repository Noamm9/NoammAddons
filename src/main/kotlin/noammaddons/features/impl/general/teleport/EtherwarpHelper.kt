package noammaddons.features.impl.general.teleport

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.MathUtils
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.getLook
import noammaddons.utils.MathUtils.multiply
import noammaddons.utils.ServerPlayer
import java.util.*
import kotlin.math.*

object EtherwarpHelper {
    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?) {
        val vec: Vec3?
            get() = pos?.let { Vec3(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }

        companion object {
            val NONE = EtherPos(false, null)
        }
    }

    const val EYE_HEIGHT = 1.62

    fun getEtherPos(pos: Vec3, rotation: MathUtils.Rotation, distance: Double = 60.0, returnEnd: Boolean = false): EtherPos {
        val startPos = pos.add(0.0, EYE_HEIGHT - if (ServerPlayer.player.sneaking) 0.08 else 0.0, 0.0)
        val endPos = getLook(rotation.yaw, rotation.pitch).normalize().multiply(distance).add(startPos)
        val result = traverseVoxels(startPos, endPos)

        return if (result != null) EtherPos(true, result)
        else if (returnEnd) EtherPos(true, BlockPos(endPos))
        else EtherPos.NONE
    }

    /**
     * Traverses voxels from start to end and returns the first block that satisfies the predicate.
     * @author Bloom, translated by [@Noamm9]
     */
    private fun traverseVoxels(start: Vec3, end: Vec3): BlockPos? {
        var x = floor(start.xCoord).toInt()
        var y = floor(start.yCoord).toInt()
        var z = floor(start.zCoord).toInt()

        val endX = floor(end.xCoord).toInt()
        val endY = floor(end.yCoord).toInt()
        val endZ = floor(end.zCoord).toInt()

        val dirX = end.xCoord - start.xCoord
        val dirY = end.yCoord - start.yCoord
        val dirZ = end.zCoord - start.zCoord

        val stepX = sign(dirX).toInt()
        val stepY = sign(dirY).toInt()
        val stepZ = sign(dirZ).toInt()

        val tDeltaX = if (dirX == 0.0) Double.POSITIVE_INFINITY else abs(1.0 / dirX)
        val tDeltaY = if (dirY == 0.0) Double.POSITIVE_INFINITY else abs(1.0 / dirY)
        val tDeltaZ = if (dirZ == 0.0) Double.POSITIVE_INFINITY else abs(1.0 / dirZ)

        var tMaxX = if (dirX == 0.0) Double.POSITIVE_INFINITY else abs((floor(start.xCoord) + max(0.0, stepX.toDouble()) - start.xCoord) / dirX)
        var tMaxY = if (dirY == 0.0) Double.POSITIVE_INFINITY else abs((floor(start.yCoord) + max(0.0, stepY.toDouble()) - start.yCoord) / dirY)
        var tMaxZ = if (dirZ == 0.0) Double.POSITIVE_INFINITY else abs((floor(start.zCoord) + max(0.0, stepZ.toDouble()) - start.zCoord) / dirZ)

        repeat(1000) {
            val currentPos = BlockPos(x, y, z)

            if (isValidEtherwarpBlock(currentPos)) return currentPos
            if (! validEtherwarpFeetIds[getBlockAt(currentPos).getBlockId()]) return null
            if (x == endX && y == endY && z == endZ) return null

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    tMaxX += tDeltaX
                    x += stepX
                }
                else {
                    tMaxZ += tDeltaZ
                    z += stepZ
                }
            }
            else {
                if (tMaxY < tMaxZ) {
                    tMaxY += tDeltaY
                    y += stepY
                }
                else {
                    tMaxZ += tDeltaZ
                    z += stepZ
                }
            }
        }

        return null
    }

    private fun isValidEtherwarpBlock(pos: BlockPos): Boolean {
        val id = getBlockAt(pos).getBlockId()

        if (id == 0 || validEtherwarpFeetIds[id]) return false
        val blockAbove = getBlockAt(pos.add(y = 1)).getBlockId()
        if (! validEtherwarpFeetIds[blockAbove]) return false

        val blockAboveAbove = getBlockAt(pos.add(y = 2)).getBlockId()
        return validEtherwarpFeetIds[blockAboveAbove]
    }

    private val validEtherwarpFeetIds = BitSet(500).apply {
        arrayOf(
            0, 6, 8, 9, 10, 11, 30, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59,
            65, 66, 69, 75, 76, 77, 78, 83, 90, 93, 94, 104, 105,
            106, 115, 131, 132, 140, 141, 142, 143, 144, 149,
            150, 157, 171, 175, 176, 177, 397, 127
        ).forEach { set(it) }
    }
}