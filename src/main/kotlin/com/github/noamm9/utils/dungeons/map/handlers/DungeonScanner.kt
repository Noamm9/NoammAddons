package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.utils.MathUtils.Vec3
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.websocket.WebSocket
import com.github.noamm9.websocket.packets.S2CPacketDungeonDoor
import com.github.noamm9.websocket.packets.S2CPacketDungeonRoom
import net.minecraft.world.level.block.Blocks

object DungeonScanner {
    const val startX = - 185
    const val startZ = - 185

    const val roomSize = 32
    const val halfRoomSize = 15

    val clayBlocksCorners = listOf(
        Pair(- halfRoomSize, - halfRoomSize),
        Pair(halfRoomSize, - halfRoomSize),
        Pair(halfRoomSize, halfRoomSize),
        Pair(- halfRoomSize, halfRoomSize)
    )

    private var lastScanTime = 0L
    private var isScanning = false
    var hasScanned = false

    val shouldScan get() = ! isScanning && ! hasScanned && System.currentTimeMillis() - lastScanTime >= 250

    fun scan() {
        isScanning = true
        var allChunksLoaded = true

        for (x in 0 .. 10) {
            for (z in 0 .. 10) {
                val wX = startX + x * (roomSize shr 1)
                val wZ = startZ + z * (roomSize shr 1)

                if (! WorldUtils.isChunkLoaded(wX, wZ)) {
                    allChunksLoaded = false
                    continue
                }

                val roofHeight = ScanUtils.getHighestY(wX, wZ)
                if (roofHeight <= 0) continue

                val roomInGrid = DungeonInfo.dungeonList[x + z * 11]
                if (roomInGrid !is Unknown && (roomInGrid as? Room)?.data?.name != "Unknown") continue

                scanRoom(wX, wZ, z, x, roofHeight)?.let { room ->
                    DungeonInfo.dungeonList[z * 11 + x] = room
                    if (DungeonListener.dungeonTeammatesNoSelf.isEmpty()) return@let

                    if (room is Room && room.data.name != "Unknown") {
                        WebSocket.send(S2CPacketDungeonRoom(room.data.name, wX, wZ, x, z, room.core, room.isSeparator))
                    }

                    if (room is Door) {
                        WebSocket.send(S2CPacketDungeonDoor(wX, wZ, x, z, room.type))
                    }
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

    fun findMimicRoom(): UniqueRoom? {
        WorldUtils.getBlockEntityList()
            .filter { WorldUtils.getStateAt(it).`is`(Blocks.TRAPPED_CHEST) }
            .groupingBy { ScanUtils.getRoomFromPos(Vec3(it.x, it.y, it.z))?.data?.name }
            .eachCount()
            .forEach { (roomName, trappedCount) ->
                if (roomName == null) return@forEach

                val roomEntry = DungeonInfo.uniqueRooms.entries.find {
                    it.key == roomName && it.value.data.trappedChests < trappedCount
                }

                if (roomEntry != null) return roomEntry.value
            }

        return null
    }

    private fun scanRoom(x: Int, z: Int, row: Int, column: Int, roofHeight: Int): Tile? {
        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            // Scanning a room
            rowEven && columnEven -> {
                val roomCore = ScanUtils.getCore(x, z)
                Room(x, z, ScanUtils.getRoomData(roomCore) ?: return null).apply {
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
            (roofHeight == 74 || roofHeight == 82 || roofHeight == 73 || roofHeight == 81) -> {
                Door(
                    x, z,
                    type = when (WorldUtils.getBlockAt(x, 69, z)) {
                        Blocks.COAL_BLOCK -> {
                            DungeonInfo.witherDoors ++
                            DoorType.WITHER
                        }

                        Blocks.INFESTED_CHISELED_STONE_BRICKS -> DoorType.ENTRANCE
                        Blocks.RED_TERRACOTTA -> DoorType.BLOOD
                        else -> DoorType.NORMAL
                    }
                )
            }

            // Connection between large rooms
            else -> DungeonInfo.dungeonList[if (rowEven) row * 11 + column - 1 else (row - 1) * 11 + column].let {
                when {
                    it !is Room -> null
                    it.data.type == RoomType.ENTRANCE -> Door(x, z, DoorType.ENTRANCE)
                    else -> Room(x, z, it.data).apply {
                        isSeparator = true
                        addToUnique(row, column)
                    }
                }
            }
        }
    }
}