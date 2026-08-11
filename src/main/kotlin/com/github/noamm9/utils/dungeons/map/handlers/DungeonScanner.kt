package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.websocket.WebSocket
import com.github.noamm9.websocket.packets.S2CPacketDungeonDoor
import com.github.noamm9.websocket.packets.S2CPacketDungeonRoom
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

object DungeonScanner: ISelfInit {
    val dungeonList = Array<Tile>(121) { Unknown(0, 0) }
    val uniqueRooms = mutableMapOf<String, UniqueRoom>()
    var mimicRoom: UniqueRoom? = null

    var witherDoors = 0
    var cryptCount = 0
    var secretCount = 0

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
    var hasScanned = false

    override fun init() {
        EventBus.register<WorldChangeEvent> {
            dungeonList.fill(Unknown(0, 0))
            uniqueRooms.clear()
            mimicRoom = null
            hasScanned = false
            witherDoors = 0
            cryptCount = 0
            secretCount = 0
        }

        EventBus.register<TickEvent.End> {
            val floor = LocationUtils.dungeonFloorNumber ?: return@register
            if (LocationUtils.inBoss) return@register

            if (System.currentTimeMillis() - lastScanTime >= 250) scan()

            if (ScoreCalculation.mimicKilled) return@register
            if (mimicRoom != null) return@register
            if (floor < 6) return@register

            findMimicRoom()
        }
    }

    private fun scan() {
        var allChunksLoaded = true

        if (! hasScanned) for (x in 0 .. 10) for (z in 0 .. 10) {
            val wX = startX + x * (roomSize shr 1)
            val wZ = startZ + z * (roomSize shr 1)

            if (! WorldUtils.isChunkLoaded(wX, wZ)) {
                allChunksLoaded = false
                continue
            }

            val inGrid = dungeonList[x + z * 11]
            if (inGrid !is Unknown && (inGrid as? RoomTile)?.data?.isUnknown() != true) continue
            val roofHeight = ScanUtils.getHighestY(wX, wZ).takeUnless { it <= 0 } ?: continue

            scanTile(wX, wZ, z, x, roofHeight)?.let { tile ->
                dungeonList[z * 11 + x] = tile
                EventBus.post(DungeonEvent.TileScannedEvent(tile))

                if (DungeonListener.dungeonTeammatesNoSelf.isEmpty()) return@let

                if (tile is RoomTile && ! tile.data.isUnknown()) {
                    WebSocket.send(S2CPacketDungeonRoom(tile.data.name, wX, wZ, x, z, tile.isSeparator))
                }

                if (tile is DoorTile) {
                    WebSocket.send(S2CPacketDungeonDoor(wX, wZ, x, z, tile.type))
                }
            }
        }

        lastScanTime = System.currentTimeMillis()
        uniqueRooms.values.forEach(UniqueRoom::findRotation)
        if (allChunksLoaded) hasScanned = true
    }

    private fun findMimicRoom() {
        val roomChests = mutableMapOf<UniqueRoom, MutableList<BlockPos>>()

        for (pos in WorldUtils.getBlockEntityList()) {
            if (! WorldUtils.getStateAt(pos).`is`(Blocks.TRAPPED_CHEST)) continue

            val room = ScanUtils.getRoomFromPos(pos.toVec()) ?: continue
            val chests = roomChests.getOrPut(room) { mutableListOf() }.apply { add(pos) }

            if (chests.size > room.data.trappedChests) {
                room.trappedChestPositions = chests
                room.hasMimic = true
                mimicRoom = room
            }
        }
    }

    private fun scanTile(x: Int, z: Int, row: Int, column: Int, roofHeight: Int): Tile? {
        val rowEven = row and 1 == 0
        val columnEven = column and 1 == 0

        return when {
            // Scanning a room
            rowEven && columnEven -> {
                val roomCore = ScanUtils.getCore(x, z)
                RoomTile(x, z, ScanUtils.getRoomData(roomCore) ?: return null).apply {
                    addToUnique(row, column)
                    uniqueRoom?.highestBlock = roofHeight
                }
            }

            // Can only be the center "block" of a 2x2 room.
            ! rowEven && ! columnEven -> {
                dungeonList[column - 1 + (row - 1) * 11].let {
                    if (it is RoomTile) {
                        RoomTile(x, z, it.data).apply {
                            isSeparator = true
                            addToUnique(row, column)
                        }
                    }
                    else null
                }
            }

            // Doorway between rooms
            roofHeight.equalsOneOf(73, 74, 81, 82) -> {
                val doorBlock = WorldUtils.getBlockAt(x, 69, z)
                val type = DoorType.entries.find { it.source == doorBlock } ?: DoorType.NORMAL
                if (type == DoorType.WITHER) witherDoors ++
                DoorTile(x, z, type)
            }

            // Connection between large rooms
            else -> dungeonList[if (rowEven) row * 11 + column - 1 else (row - 1) * 11 + column].let {
                when {
                    it !is RoomTile -> null
                    it.data.type == RoomType.ENTRANCE -> DoorTile(x, z, DoorType.ENTRANCE)
                    else -> RoomTile(x, z, it.data).apply {
                        isSeparator = true
                        addToUnique(row, column)
                    }
                }
            }
        }
    }
}