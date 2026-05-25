package com.github.noamm9.utils.items

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.*
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.jvm.optionals.getOrDefault
import kotlin.math.*

object EtherwarpHelper {
    private val modernWorlds = setOf(
        WorldType.Galatea, WorldType.GoldMine, WorldType.Hub,
        WorldType.End, WorldType.Park, WorldType.SpiderDen,
        WorldType.TheBarn
    )

    private const val EYE_HEIGHT = 1.62
    private inline val SNEAK_OFFSET get() = if (LocationUtils.world in modernWorlds) 0.35 else 0.08

    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?) {
        val vec = pos?.let(::Vec3)

        companion object {
            val NONE = EtherPos(false, null)
        }
    }

    fun getEtherwarpDistance(stack: ItemStack): Double? {
        if (stack.skyblockId.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val nbt = stack.customData
            if (nbt.getByte("ethermerge").orElse(0) != 1.toByte()) return null
            val tuners = nbt.getByte("tuned_transmission").getOrDefault(0).toInt()
            return 57.0 + tuners
        }
        return null
    }

    fun getEtherPos(pos: Vec3, lookVec: Vec3, distance: Double): EtherPos {
        val player = mc.player ?: return EtherPos.NONE
        val startPos = getZeroPingCameraPos(pos).add(y = EYE_HEIGHT - if (player.isCrouching) SNEAK_OFFSET else 0.0)
        val endPos = startPos.add(lookVec.scale(distance))
        return traverseVoxels(startPos, endPos)
    }

    private fun getZeroPingCameraPos(fallback: Vec3): Vec3 {
        //#if CHEAT
        val noRotate = com.github.noamm9.features.impl.misc.NoRotate
        if (! noRotate.enabled) return fallback
        val pendingTeleport = noRotate.pendingTeleports.lastOrNull() ?: return fallback
        val config = noRotate.zeroPingCamera.value.values.toList()
        if (! config[pendingTeleport.info.type.ordinal]) return fallback
        return pendingTeleport.position
        //#else
        //$return fallback
        //#endif
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

        val invDirX = if (dirX != 0.0) 1.0 / dirX else Double.MAX_VALUE
        val invDirY = if (dirY != 0.0) 1.0 / dirY else Double.MAX_VALUE
        val invDirZ = if (dirZ != 0.0) 1.0 / dirZ else Double.MAX_VALUE

        val tDeltaX = abs(invDirX * stepX)
        val tDeltaY = abs(invDirY * stepY)
        val tDeltaZ = abs(invDirZ * stepZ)

        var tMaxX = abs((x + max(stepX, 0) - start.x) * invDirX)
        var tMaxY = abs((y + max(stepY, 0) - start.y) * invDirY)
        var tMaxZ = abs((z + max(stepZ, 0) - start.z) * invDirZ)

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

    private fun isValidEtherwarpBlock(pos: BlockPos, chunk: LevelChunk): Boolean {
        val level = mc.level ?: return false
        if (isPassable(pos, chunk)) return false

        val state = chunk.getBlockState(pos)
        val collisionTop = state.getCollisionShape(level, pos, CollisionContext.empty()).max(Direction.Axis.Y)
        val clearanceBaseY = pos.y + max(1, ceil(collisionTop).toInt())

        val feetPos = BlockPos(pos.x, clearanceBaseY, pos.z)
        if (! isPassable(feetPos, chunk) || isBlocksFeet(feetPos, chunk)) return false

        val headPos = BlockPos(pos.x, clearanceBaseY + 1, pos.z)
        return ! (! isPassable(headPos, chunk) || isBlocksFeet(headPos, chunk))
    }

    private fun isBlocksFeet(pos: BlockPos, chunk: LevelChunk): Boolean {
        return when (chunk.getBlockState(pos).block) {
            is SkullBlock, is WallSkullBlock -> true
            is FlowerPotBlock -> true
            is LadderBlock -> true
            is VineBlock -> true
            else -> false
        }
    }

    private fun isPassable(pos: BlockPos, chunk: LevelChunk): Boolean {
        val level = mc.level ?: return true
        val state = chunk.getBlockState(pos)
        return when (state.block) {
            is FlowerPotBlock -> true
            is LadderBlock -> true
            is SignBlock -> false
            else -> state.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty
        }
    }
}