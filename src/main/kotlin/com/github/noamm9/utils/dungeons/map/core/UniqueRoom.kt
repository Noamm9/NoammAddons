package com.github.noamm9.utils.dungeons.map.core

import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import kotlin.math.max

class UniqueRoom(arrX: Int, arrY: Int, room: Room) {
    private var topLeft = Pair(arrX, arrY)
    private var center = Pair(arrX, arrY)

    var mainRoom = room
    val centerPos = BlockPos(mainRoom.x, 0, mainRoom.z)
    val tiles = mutableListOf(room)

    val data = room.data
    val name = data.name
    val cacheSplitName = name.split(" ")

    var hasMimic = false
    var trappedChestPositions = emptyList<BlockPos>()
    var foundSecrets = 0
        set(value) {
            field = value
            cachedTextMaxWidth = - 1f
        }

    var highestBlock: Int? = null
    var corner: BlockPos? = null
    var rotation: Int? = null

    init {
        DungeonInfo.cryptCount += room.data.crypts
        DungeonInfo.secretCount += room.data.secrets
    }

    private var cachedScale = - 1f
    private var cachedTextMaxWidth = - 1f

    private var boundsDirty = true
    var cachedMaxWidth = 0f
    var cachedMaxHeight = 0f

    fun updateBounds(roomSize: Float, gapSize: Float) {
        if (! boundsDirty) return
        boundsDirty = false

        var maxWidth = roomSize
        var maxHeight = roomSize

        if (data.shape != "L" && data.shape != "1x1" && data.shape != "2x2") {
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

            val tilesWide = maxX - minX + 1
            val tilesTall = maxZ - minZ + 1
            maxWidth = (tilesWide * roomSize) + (max(0, tilesWide - 1) * gapSize)
            maxHeight = (tilesTall * roomSize) + (max(0, tilesTall - 1) * gapSize)
        }
        if (data.shape == "L") maxWidth = roomSize * 2
        else if (data.shape == "2x2") {
            maxWidth = roomSize * 2
            maxHeight = roomSize * 2
        }

        cachedMaxWidth = maxWidth
        cachedMaxHeight = maxHeight
        cachedTextMaxWidth = - 1f
    }

    fun updateTextScale(baseScale: Float, showSecrets: Boolean, secretsText: String): Float {
        if (cachedTextMaxWidth >= 0f && cachedScale == baseScale) return cachedTextMaxWidth

        var maxLineW = 0f
        for (line in cacheSplitName) {
            val w = line.width() * baseScale
            if (w > maxLineW) maxLineW = w
        }

        if (showSecrets) {
            val w = secretsText.width() * baseScale
            if (w > maxLineW) maxLineW = w
        }

        cachedTextMaxWidth = maxLineW
        cachedScale = baseScale
        return maxLineW
    }

    fun addTile(x: Int, y: Int, tile: Room) {
        boundsDirty = true

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
            corner = BlockPos(mainRoom.x - 15, 0, mainRoom.z - 15)
            rotation = 0
            return
        }

        val y = highestBlock ?: return
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

        val h = DungeonScanner.halfRoomSize

        val primaryCornersX = intArrayOf(minX - h, maxX + h, maxX + h, minX - h)
        val primaryCornersZ = intArrayOf(minZ - h, minZ - h, maxZ + h, maxZ + h)

        for (i in 0 .. 3) {
            mutablePos.set(primaryCornersX[i], y, primaryCornersZ[i])
            if (WorldUtils.getBlockAt(mutablePos) == Blocks.BLUE_TERRACOTTA) {
                setRotationAndCorner(i, mutablePos)
                return
            }
        }

        for (tile in tiles) for (i in DungeonScanner.clayBlocksCorners.indices) {
            val offset = DungeonScanner.clayBlocksCorners[i]
            val cx = tile.x + offset.first
            val cz = tile.z + offset.second

            mutablePos.set(cx, y, cz)
            if (WorldUtils.getBlockAt(mutablePos) == Blocks.BLUE_TERRACOTTA) {
                setRotationAndCorner(i, mutablePos)
                return
            }
        }
    }

    private fun setRotationAndCorner(index: Int, pos: BlockPos) {
        corner = BlockPos(pos.x, 0, pos.z)
        rotation = index * 90
    }
}