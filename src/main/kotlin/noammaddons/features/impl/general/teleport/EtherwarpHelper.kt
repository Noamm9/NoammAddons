package noammaddons.features.impl.general.teleport

import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.MathUtils
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.MathUtils.floor
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

    fun getPlayerEyeHeight() = 1.62 + if (ServerPlayer.player.sneaking) - .08 else .0

    fun getLook(yaw: Float = mc.thePlayer?.rotationYaw ?: 0f, pitch: Float = mc.thePlayer?.rotationPitch ?: 0f): Vec3 {
        val f2 = - cos(- pitch * 0.017453292f).toDouble()
        return Vec3(
            sin(- yaw * 0.017453292f - 3.1415927f) * f2,
            sin(- pitch * 0.017453292f).toDouble(),
            cos(- yaw * 0.017453292f - 3.1415927f) * f2
        )
    }

    fun Vec3.multiply(factor: Double) = Vec3(
        xCoord * factor,
        yCoord * factor,
        zCoord * factor
    )

    fun getEtherPos(pos: Vec3, rotation: MathUtils.Rotation, distance: Double = 60.0, returnEnd: Boolean = false): EtherPos {
        val startPos: Vec3 = pos.add(y = getPlayerEyeHeight())
        val endPos = getLook(rotation.yaw, rotation.pitch).normalize().multiply(factor = distance).add(startPos)
        return traverseVoxels(startPos, endPos).takeUnless { it == EtherPos.NONE && returnEnd } ?: EtherPos(true, BlockPos(endPos))
    }

    /**
     * Traverses voxels from start to end and returns the first non-air block it hits.
     * @author Bloom
     */
    private fun traverseVoxels(start: Vec3, end: Vec3): EtherPos {
        val (x0, y0, z0) = start.destructured()
        val (x1, y1, z1) = end.destructured()

        var (x, y, z) = start.floor().destructured()
        val (endX, endY, endZ) = end.floor().destructured()

        val dirX = x1 - x0
        val dirY = y1 - y0
        val dirZ = z1 - z0

        val stepX = sign(dirX).toInt()
        val stepY = sign(dirY).toInt()
        val stepZ = sign(dirZ).toInt()

        val invDirX = if (dirX != 0.0) 1.0 / dirX else Double.MAX_VALUE
        val invDirY = if (dirY != 0.0) 1.0 / dirY else Double.MAX_VALUE
        val invDirZ = if (dirZ != 0.0) 1.0 / dirZ else Double.MAX_VALUE

        val tDeltaX = abs(invDirX * stepX)
        val tDeltaY = abs(invDirY * stepY)
        val tDeltaZ = abs(invDirZ * stepZ)

        var tMaxX = abs((x + max(stepX, 0) - x0) * invDirX)
        var tMaxY = abs((y + max(stepY, 0) - y0) * invDirY)
        var tMaxZ = abs((z + max(stepZ, 0) - z0) * invDirZ)

        repeat(1000) {
            val chunk = mc.theWorld?.chunkProvider?.provideChunk(x.toInt() shr 4, z.toInt() shr 4) ?: return EtherPos.NONE
            val currentBlock = chunk.getBlock(BlockPos(x, y, z)).getBlockId()

            if (currentBlock != 0) {
                if (validEtherwarpFeetIds.get(currentBlock)) return EtherPos(false, BlockPos(x, y, z))
                if (currentBlock.equalsOneOf(8, 9, 10, 11)) return EtherPos(false, BlockPos(x, y, z))

                val footBlockId = Block.getIdFromBlock(chunk.getBlock(BlockPos(x, y + 1, z)))
                if (! validEtherwarpFeetIds.get(footBlockId)) return EtherPos(false, BlockPos(x, y, z))

                val headBlockId = Block.getIdFromBlock(chunk.getBlock(BlockPos(x, y + 2, z)))
                if (! validEtherwarpFeetIds.get(headBlockId)) return EtherPos(false, BlockPos(x, y, z))

                return EtherPos(true, BlockPos(x, y, z))
            }

            if (x == endX && y == endY && z == endZ) return EtherPos.NONE

            when {
                tMaxX <= tMaxY && tMaxX <= tMaxZ -> {
                    tMaxX += tDeltaX
                    x += stepX
                }

                tMaxY <= tMaxZ -> {
                    tMaxY += tDeltaY
                    y += stepY
                }

                else -> {
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