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
import noammaddons.utils.Utils.equalsOneOf
import java.util.*
import kotlin.math.*


object EtherwarpHelper {
    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?) {
        inline val vec: Vec3? get() = pos?.let { Vec3(it) }

        companion object {
            val NONE = EtherPos(false, null)
        }
    }

    const val EYE_HEIGHT = 1.62

    fun getEtherPos(pos: Vec3, rotation: MathUtils.Rotation, distance: Double = 60.0, returnEnd: Boolean = false): EtherPos {
        val startPos: Vec3 = pos.add(y = EYE_HEIGHT - if (ServerPlayer.player.sneaking) .08 else .0)
        val endPos = getLook(rotation.yaw, rotation.pitch).normalize().multiply(factor = distance).add(startPos)
        return traverseVoxels(startPos, endPos).takeUnless { it == EtherPos.NONE && returnEnd } ?: EtherPos(true, BlockPos(endPos))
    }

    /**
     * Traverses voxels from start to end and returns the first non-air block it hits.
     * @author Bloom
     */
    private fun traverseVoxels(start: Vec3, end: Vec3): EtherPos {
        return traverseVoxels(start.xCoord, start.yCoord, start.zCoord, end.xCoord, end.yCoord, end.zCoord)
    }

    private fun traverseVoxels(x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double): EtherPos {
        var x = floor(x0).toInt()
        var y = floor(y0).toInt()
        var z = floor(z0).toInt()

        val endX = floor(x1).toInt()
        val endY = floor(y1).toInt()
        val endZ = floor(z1).toInt()

        val dirX = x1 - x0
        val dirY = y1 - y0
        val dirZ = z1 - z0

        val stepX = sign(dirX).toInt()
        val stepY = sign(dirY).toInt()
        val stepZ = sign(dirZ).toInt()

        val tDeltaX = if (dirX == 0.0) Double.POSITIVE_INFINITY else min(abs(1.0 / dirX), 1.0)
        val tDeltaY = if (dirY == 0.0) Double.POSITIVE_INFINITY else min(abs(1.0 / dirY), 1.0)
        val tDeltaZ = if (dirZ == 0.0) Double.POSITIVE_INFINITY else min(abs(1.0 / dirZ), 1.0)

        var tMaxX = if (dirX == 0.0) Double.POSITIVE_INFINITY else abs((floor(x0) + max(0.0, stepX.toDouble()) - x0) / dirX)
        var tMaxY = if (dirY == 0.0) Double.POSITIVE_INFINITY else abs((floor(y0) + max(0.0, stepY.toDouble()) - y0) / dirY)
        var tMaxZ = if (dirZ == 0.0) Double.POSITIVE_INFINITY else abs((floor(z0) + max(0.0, stepZ.toDouble()) - z0) / dirZ)

        repeat(1000) {
            val currentPos = BlockPos(x, y, z)
            val currentBlock = getBlockAt(currentPos).getBlockId()

            if (currentBlock != 0) {
                if (validEtherwarpFeetIds.get(currentBlock)) return EtherPos(false, currentPos)
                if (currentBlock.equalsOneOf(8, 9, 10, 11)) return EtherPos(false, currentPos)

                if (! validEtherwarpFeetIds.get(currentBlock)) {

                    val footBlock = getBlockAt(x, y + 1, z).getBlockId()
                    if (! validEtherwarpFeetIds.get(footBlock)) return EtherPos(false, currentPos)

                    val headBlock = getBlockAt(x, y + 2, z).getBlockId()
                    if (! validEtherwarpFeetIds.get(headBlock)) return EtherPos(false, currentPos)

                    return EtherPos(true, currentPos)
                }
                return EtherPos(false, currentPos)

            }

            if (x == endX && y == endY && z == endZ) return EtherPos.NONE

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

        return EtherPos.NONE
    }


    private val validEtherwarpFeetIds = BitSet(176).apply {
        arrayOf(
            0, 6, 9, 11, 30, 31, 32, 36, 37, 38, 39, 40, 50, 51, 55, 59, 65, 66, 69, 76, 77, 78,
            93, 94, 104, 105, 106, 111, 115, 131, 132, 140, 141, 142, 143, 144, 149, 150, 157, 171, 175
        ).forEach { set(it) }
    }
}