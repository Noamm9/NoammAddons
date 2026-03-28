package com.github.noamm9.utils.items

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.piston.PistonHeadBlock
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sign

object EtherwarpHelper {
    private const val EYE_HEIGHT = 1.62
    private inline val SNEAK_OFFSET: Double
        get() {
            if (LocationUtils.world.equalsOneOf(WorldType.Galatea, WorldType.Home, WorldType.Park, WorldType.SpiderDen)) {
                return 0.35
            }

            return 0.08
        }

    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?) {
        val vec = pos?.let { Vec3(it) }

        companion object {
            val NONE = EtherPos(false, null)
        }
    }

    fun getEtherwarpDistance(stack: ItemStack): Double? {
        if (stack.skyblockId.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val nbt = stack.customData
            if (nbt.getByte("ethermerge").orElse(0) != 1.toByte()) return null
            val tuners = nbt.getByte("tuned_transmission").orElse(0).toInt()
            return 57.0 + tuners
        }
        return null
    }

    fun getEtherPos(pos: Vec3, lookVec: Vec3, distance: Double): EtherPos {
        val player = mc.player ?: return EtherPos.NONE
        val startPos = pos.add(y = EYE_HEIGHT - if (player.isCrouching) SNEAK_OFFSET else 0.0)
        val endPos = startPos.add(lookVec.scale(distance))
        return traverseVoxels(startPos, endPos)
    }

    private fun traverseVoxels(start: Vec3, end: Vec3): EtherPos {
        var x = floor(start.x).toInt()
        var y = floor(start.y).toInt()
        var z = floor(start.z).toInt()

        val endX = floor(end.x).toInt()
        val endY = floor(end.y).toInt()
        val endZ = floor(end.z).toInt()

        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val dirZ = end.z - start.z

        val stepX = sign(dirX).toInt()
        val stepY = sign(dirY).toInt()
        val stepZ = sign(dirZ).toInt()

        val tDeltaX = abs(1.0 / dirX)
        val tDeltaY = abs(1.0 / dirY)
        val tDeltaZ = abs(1.0 / dirZ)

        var tMaxX = abs((floor(start.x) + max(0.0, stepX.toDouble()) - start.x) / dirX)
        var tMaxY = abs((floor(start.y) + max(0.0, stepY.toDouble()) - start.y) / dirY)
        var tMaxZ = abs((floor(start.z) + max(0.0, stepZ.toDouble()) - start.z) / dirZ)

        val currentPos = BlockPos.MutableBlockPos()

        repeat(1000) {
            currentPos.set(x, y, z)

            val chunk = mc.level?.getChunk(
                SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(z)
            ) ?: return EtherPos.NONE

            val state = chunk.getBlockState(currentPos)

            if (isValidEtherwarpBlock(currentPos, chunk)) return EtherPos(true, currentPos)
            if (! isPassable(currentPos, chunk)) return EtherPos(false, currentPos)
            if (x == endX && y == endY && z == endZ) return if (state.isAir) EtherPos.NONE else EtherPos(false, currentPos)

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

    private fun isValidEtherwarpBlock(pos: BlockPos, chunk: LevelChunk): Boolean {
        if (isPassable(pos, chunk)) return false
        if (! isPassable(pos.above(1), chunk)) return false
        return isPassable(pos.above(2), chunk)
    }

    private fun isPassable(pos: BlockPos, chunk: LevelChunk): Boolean {
        val level = mc.level ?: return true
        val state = chunk.getBlockState(pos)
        if (extraPassable.any { it.isInstance(state.block) }) return true
        return state.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty
    }

    private val extraPassable = setOf(
        CarpetBlock::class, SkullBlock::class, WallSkullBlock::class,
        LadderBlock::class, SnowLayerBlock::class, BubbleColumnBlock::class,
        FlowerPotBlock::class, LiquidBlock::class, PistonHeadBlock::class,
        ComparatorBlock::class, RedstoneTorchBlock::class, RepeaterBlock::class
    )
}