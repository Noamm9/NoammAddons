package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.Vec3

object WorldUtils {
    fun getStateAt(pos: BlockPos) = NoammAddons.mc.level?.getBlockState(pos) ?: Blocks.AIR.defaultBlockState()
    fun getStateAt(x: Int, y: Int, z: Int) = this.getStateAt(BlockPos(x, y, z))
    fun getBlockAt(pos: BlockPos) = getStateAt(pos).block
    fun getBlockAt(vec3: Vec3) = getBlockAt(BlockPos(vec3.x.toInt(), vec3.y.toInt(), vec3.z.toInt()))
    fun getBlockAt(x: Number, y: Number, z: Number) = getBlockAt(BlockPos(x.toInt(), y.toInt(), z.toInt()))

    fun setBlockAt(pos: BlockPos, state: BlockState) = NoammAddons.mc.level?.setBlock(pos, state, 19)

    fun isChunkLoaded(x: Int, z: Int): Boolean {
        return NoammAddons.mc.level?.getChunk(x shr 4, z shr 4, ChunkStatus.FULL, false) != null
    }

    fun getBlockEntityList(): List<BlockPos> {
        val player = NoammAddons.mc.player ?: return emptyList()
        val level = NoammAddons.mc.level ?: return emptyList()
        val renderDistance = NoammAddons.mc.options.renderDistance().get()
        val pX = player.chunkPosition().x
        val pZ = player.chunkPosition().z

        return buildList {
            for (x in (pX - renderDistance) .. (pX + renderDistance)) {
                for (z in (pZ - renderDistance) .. (pZ + renderDistance)) {
                    val chunk = level.getChunk(x, z, ChunkStatus.FULL, false) ?: continue
                    addAll(chunk.blockEntitiesPos)
                }
            }
        }
    }
}