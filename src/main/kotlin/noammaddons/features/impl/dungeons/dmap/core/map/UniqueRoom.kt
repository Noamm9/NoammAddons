package noammaddons.features.impl.dungeons.dmap.core.map

import net.minecraft.util.BlockPos
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapConfig
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonScanner.halfRoomSize
import noammaddons.utils.BlockUtils.getBlockId
import noammaddons.utils.BlockUtils.getMetadata
import noammaddons.utils.BlockUtils.getStateAt
import noammaddons.utils.MathUtils.add


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

    fun getCheckmarkPosition() = if (DungeonMapConfig.centerStyle.value) center else topLeft

    fun findRotation() {
        if (mainRoom.data.type == RoomType.FAIRY) {
            rotation = 0
            corner = BlockPos(mainRoom.x - 15, 0, mainRoom.z - 15)
            return
        }

        val yLevel = highestBlock !!.toDouble()
        val scannedPositions = HashSet<BlockPos>()

        val minX = tiles.minOf { it.x }
        val maxX = tiles.maxOf { it.x }
        val minZ = tiles.minOf { it.z }
        val maxZ = tiles.maxOf { it.z }

        listOf(
            Pair(minX - halfRoomSize, minZ - halfRoomSize),
            Pair(maxX + halfRoomSize, minZ - halfRoomSize),
            Pair(maxX + halfRoomSize, maxZ + halfRoomSize),
            Pair(minX - halfRoomSize, maxZ + halfRoomSize)
        ).forEachIndexed { i, (x, z) ->
            val pos = BlockPos(x, yLevel, z)
            scannedPositions.add(pos)
            getStateAt(pos).takeIf { it.getBlockId() == 159 && it.getMetadata() == 11 }?.let {
                rotation = i * 90
                corner = pos.add(y = - yLevel)
                return
            }
        }

        for ((x, z) in tiles.map { Pair(it.x, it.z) }) {
            DungeonScanner.clayBlocksCorners.forEachIndexed { i, (rx, rz) ->
                val pos = BlockPos(x + rx, yLevel, z + rz)
                if (scannedPositions.contains(pos)) return@forEachIndexed
                getStateAt(pos).takeIf { it.getBlockId() == 159 && it.getMetadata() == 11 }?.let {
                    rotation = i * 90
                    corner = pos.add(y = - yLevel)
                    return
                }
            }
        }
    }
}
