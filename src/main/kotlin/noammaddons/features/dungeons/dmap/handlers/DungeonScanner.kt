package noammaddons.features.dungeons.dmap.handlers

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import noammaddons.features.dungeons.dmap.core.map.Door
import noammaddons.features.dungeons.dmap.core.map.DoorType
import noammaddons.features.dungeons.dmap.core.map.Room
import noammaddons.features.dungeons.dmap.core.map.RoomType
import noammaddons.features.dungeons.dmap.core.map.Tile
import noammaddons.features.dungeons.dmap.core.map.Unknown
import noammaddons.features.dungeons.dmap.handlers.DungeonScanner.scan
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.LocationUtils.dungeonFloorNumber

/**
 * Handles everything related to scanning the dungeon. Running [scan] will update the instance of [DungeonInfo].
 */
object DungeonScanner {

    /**
     * The size of each dungeon room in blocks.
     */
    const val roomSize = 32

    /**
     * The starting coordinates to start scanning (the north-west corner).
     */
    const val startX = - 185
    const val startZ = - 185

    private var lastScanTime = 0L
    var isScanning = false
    var hasScanned = false

    val shouldScan: Boolean
        get() = ! isScanning && ! hasScanned && System.currentTimeMillis() - lastScanTime >= 250 && dungeonFloorNumber != null

    fun scan() {
        isScanning = true
        var allChunksLoaded = true

        // Scans the dungeon in a 11x11 grid.
        for (x in 0 .. 10) {
            for (z in 0 .. 10) {
                // Translates the grid index into world position.
                val xPos = startX + x * (roomSize shr 1)
                val zPos = startZ + z * (roomSize shr 1)

                if (! mc.theWorld.getChunkFromChunkCoords(xPos shr 4, zPos shr 4).isLoaded) {
                    // The room being scanned has not been loaded in.
                    allChunksLoaded = false
                    continue
                }

                // This room has already been added in a previous scan.
                if (DungeonInfo.dungeonList[x + z * 11]
                        .run { this !is Unknown && (this as? Room)?.data?.name != "Unknown" }
                ) continue

                scanRoom(xPos, zPos, z, x)?.let {
                    DungeonInfo.dungeonList[z * 11 + x] = it
                }
            }
        }

        if (allChunksLoaded) {
            DungeonInfo.roomCount = DungeonInfo.dungeonList.filter { it is Room && ! it.isSeparator }.size
            hasScanned = true
        }

        lastScanTime = System.currentTimeMillis()
        isScanning = false
    }

    private fun scanRoom(x: Int, z: Int, row: Int, column: Int): Tile? {
        val height = mc.theWorld.getChunkFromChunkCoords(x shr 4, z shr 4).getHeightValue(x and 15, z and 15)
        if (height == 0) return null

        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            // Scanning a room
            rowEven && columnEven -> {
                val roomCore = noammaddons.utils.ScanUtils.getCore(x, z)
                Room(x, z, noammaddons.utils.ScanUtils.getRoomData(roomCore) ?: return null).apply {
                    core = roomCore
                    addToUnique(row, column)
                }
            }

            // Can only be the center "block" of a 2x2 room.
            ! rowEven && ! columnEven -> {
                DungeonInfo.dungeonList[column - 1 + (row - 1) * 11].let {
                    if (it is Room) {
                        Room(x, z, it.data).apply {
                            isSeparator = true
                            addToUnique(row, column)
                        }
                    }
                    else null
                }
            }

            // Doorway between rooms
            // Old trap has a single block at 82
            height == 74 || height == 82 -> {
                Door(
                    x, z,
                    // Finds door type from door block
                    type = when (getBlockAt(BlockPos(x, 69, z))) {
                        Blocks.coal_block -> {
                            DungeonInfo.witherDoors ++
                            DoorType.WITHER
                        }

                        Blocks.monster_egg -> DoorType.ENTRANCE
                        Blocks.stained_hardened_clay -> DoorType.BLOOD
                        else -> DoorType.NORMAL
                    }
                )
            }

            // Connection between large rooms
            else -> DungeonInfo.dungeonList[if (rowEven) row * 11 + column - 1 else (row - 1) * 11 + column].let {
                when {
                    it !is Room -> null
                    it.data.type == RoomType.ENTRANCE -> Door(x, z, DoorType.ENTRANCE)
                    else -> Room(x, z, it.data).apply { isSeparator = true }
                }
            }
        }
    }
}
