package com.github.noamm9.utils.dungeons.map.core

import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import kotlin.math.max

class UniqueRoom(arrX: Int, arrY: Int, roomTile: RoomTile) {
    private var topLeft = Pair(arrX, arrY)
    private var center = Pair(arrX, arrY)
    val tiles = mutableListOf(roomTile)

    var mainRoom = roomTile
    val data = roomTile.data
    val name = data.name
    val cacheSplitName = name.split(" ")

    val centerPos = BlockPos(mainRoom.x, 0, mainRoom.z)
    var highestBlock: Int? = null
    var clayPos: BlockPos? = null
    var rotation: Int? = null

    var hasMimic = false
    var trappedChestPositions = emptyList<BlockPos>()
    var foundSecrets = 0
        set(value) {
            field = value
            cachedTextMaxWidth = - 1f
        }

    init {
        DungeonScanner.cryptCount += roomTile.data.crypts
        DungeonScanner.secretCount += roomTile.data.secrets
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

        if (data.shape != RoomShape.SL && data.shape != RoomShape.S1x1 && data.shape != RoomShape.S2x2) {
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
        if (data.shape == RoomShape.SL) maxWidth = roomSize * 2
        else if (data.shape == RoomShape.S2x2) {
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

    fun addTile(x: Int, y: Int, tile: RoomTile) {
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

        val roomTiles = tiles.mapNotNull { tile -> if (tile.isSeparator) null else tile.getGridPos() }.ifEmpty { return }
        val xRooms = roomTiles.groupBy { it.second }.entries.sortedByDescending { it.value.size } // column groups
        val zRooms = roomTiles.groupBy { it.first }.entries.sortedByDescending { it.value.size }  // row groups

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
        if (rotation != null || clayPos != null) return
        if (mainRoom.data.type == RoomType.FAIRY) return setRotationAndCorner(0, BlockPos(mainRoom.x - 15, 0, mainRoom.z - 15))
        val roomTiles = tiles.filterNot { it.isSeparator }.takeUnless { it.size < data.shape.tileCount } ?: return
        val highestBlock = highestBlock ?: ScanUtils.getHighestY(mainRoom.x, mainRoom.z).takeIf { it > 0 } ?: return

        val mutablePos = BlockPos.MutableBlockPos()
        val h = DungeonScanner.halfRoomSize

        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minZ = Int.MAX_VALUE
        var maxZ = Int.MIN_VALUE

        for (tile in roomTiles) {
            if (tile.x < minX) minX = tile.x
            if (tile.x > maxX) maxX = tile.x
            if (tile.z < minZ) minZ = tile.z
            if (tile.z > maxZ) maxZ = tile.z
        }

        val primaryCornersX = intArrayOf(minX - h, maxX + h, maxX + h, minX - h)
        val primaryCornersZ = intArrayOf(minZ - h, minZ - h, maxZ + h, maxZ + h)

        for (i in 0 .. 3) {
            mutablePos.set(primaryCornersX[i], highestBlock, primaryCornersZ[i])
            if (WorldUtils.getBlockAt(mutablePos) == Blocks.BLUE_TERRACOTTA) {
                setRotationAndCorner(i, mutablePos)
                return
            }
        }

        for (tile in tiles) for (i in DungeonScanner.clayBlocksCorners.indices) {
            val (offsetX, offsetY) = DungeonScanner.clayBlocksCorners[i]
            mutablePos.set(tile.x + offsetX, highestBlock, tile.z + offsetY)
            if (! WorldUtils.isChunkLoaded(mutablePos.x, mutablePos.z)) return
            if (WorldUtils.getBlockAt(mutablePos) == Blocks.BLUE_TERRACOTTA) {
                setRotationAndCorner(i, mutablePos)
                return
            }
        }
    }

    private fun setRotationAndCorner(index: Int, pos: BlockPos) {
        clayPos = BlockPos(pos.x, 0, pos.z)
        rotation = index * 90

        ChatUtils.debug("rotation", "$name: rotation=$rotation")
    }
}