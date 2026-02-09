package com.github.noamm9.utils.dungeons.map.core

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

class UniqueRoom(arrX: Int, arrY: Int, room: Room) {
    private var topLeft = Pair(arrX, arrY)
    private var center = Pair(arrX, arrY)

    var mainRoom = room
    val tiles = mutableListOf(room)

    val data = room.data
    val name = data.name
    val cacheSplitName = name.split(" ")

    var foundSecrets = 0
    var hasMimic = false

    var highestBlock: Int? = null
    var corner: BlockPos? = null
    var rotation: Int? = null

    init {
        DungeonInfo.cryptCount += room.data.crypts
        DungeonInfo.secretCount += room.data.secrets
        when (room.data.type) {
            RoomType.TRAP -> DungeonInfo.trapType = room.data.name.split(" ")[0]
            //RoomType.PUZZLE -> Puzzle.fromName(room.data.name)?.let { DungeonInfo.puzzles.putIfAbsent(it, false) }
            else -> {}
        }
    }

    fun addTile(x: Int, y: Int, tile: Room) {
        tiles.removeIf { it.x == tile.x && it.z == tile.z }
        tiles.add(tile)

        if (x < topLeft.first || (x == topLeft.first && y < topLeft.second)) {
            topLeft = Pair(x, y)
            mainRoom = tile
        }

        if (tiles.size == 1) {
            center = Pair(x, y)
            return
        }

        val positions = tiles.mapNotNull {
            it.getArrayPosition().takeIf { (arrX, arrZ) ->
                arrX % 2 == 0 && arrZ % 2 == 0
            }
        }

        if (positions.isEmpty()) return

        val xRooms = positions.groupBy { it.first }.entries.sortedByDescending { it.value.size }
        val zRooms = positions.groupBy { it.second }.entries.sortedByDescending { it.value.size }

        center = when {
            zRooms.size == 1 || zRooms[0].value.size != zRooms[1].value.size -> {
                xRooms.sumOf { it.key } / xRooms.size to zRooms[0].key
            }

            xRooms.size == 1 || xRooms[0].value.size != xRooms[1].value.size -> {
                xRooms[0].key to zRooms.sumOf { it.key } / zRooms.size
            }

            else -> (xRooms[0].key + xRooms[1].key) / 2 to (zRooms[0].key + zRooms[1].key) / 2
        }
    }

    fun getCheckmarkPosition() = if (MapConfig.centerStyle.value) center else topLeft

    fun findRotation() {
        if (mainRoom.data.type == RoomType.FAIRY) {
            rotation = 0
            corner = BlockPos(mainRoom.x - 15, 0, mainRoom.z - 15)
            return
        }

        val y = highestBlock ?: return
        val level = NoammAddons.mc.level ?: return
        val mutablePos = BlockPos.MutableBlockPos()

        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minZ = Int.MAX_VALUE
        var maxZ = Int.MIN_VALUE

        for (tile in tiles) {
            if (tile.x < minX) minX = tile.x
            if (tile.x > maxX) maxX = tile.x
            if (tile.z < minZ) minZ = tile.z
            if (tile.z > maxZ) maxZ = tile.z
        }

        val h = DungeonScanner.halfRoomSize.toInt()

        val primaryCornersX = intArrayOf(minX - h, maxX + h, maxX + h, minX - h)
        val primaryCornersZ = intArrayOf(minZ - h, minZ - h, maxZ + h, maxZ + h)

        for (i in 0 .. 3) {
            mutablePos.set(primaryCornersX[i], y, primaryCornersZ[i])
            if (level.getBlockState(mutablePos).block == Blocks.BLUE_TERRACOTTA) {
                setRotationAndCorner(i, mutablePos)
                return
            }
        }

        for (tile in tiles) {
            for (i in DungeonScanner.clayBlocksCorners.indices) {
                val offset = DungeonScanner.clayBlocksCorners[i]
                val tx = tile.x + offset.first
                val tz = tile.z + offset.second

                mutablePos.set(tx, y, tz)
                if (level.getBlockState(mutablePos).block == Blocks.BLUE_TERRACOTTA) {
                    setRotationAndCorner(i, mutablePos)
                    return
                }
            }
        }
    }

    private fun setRotationAndCorner(index: Int, pos: BlockPos) {
        rotation = index * 90
        corner = BlockPos(pos.x, 0, pos.z)
    }
}