package com.github.noamm9.utils.dungeons.map.core

import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner.dungeonList
import com.github.noamm9.utils.dungeons.map.handlers.DungeonTree

class DoorTile(override val x: Int, override val z: Int, var type: DoorType): Tile {
    override var state = RoomState.UNDISCOVERED
    val aabb = aabb(x - 1, 69, z - 1, x + 2, 73, z + 2)
    var opened = false

    override fun getColor() = when {
        state == RoomState.UNOPENED -> MapConfig.colorUnopenedDoor.value
        type == DoorType.BLOOD -> MapConfig.colorBloodDoor.value
        type == DoorType.ENTRANCE -> MapConfig.colorEntranceDoor.value
        (type == DoorType.WITHER || DungeonTree.isFairy(this)) -> {
            if (opened && state != RoomState.UNDISCOVERED) MapConfig.colorOpenWitherDoor.value
            else MapConfig.colorWitherDoor.value
        }

        else -> {
            val coloredRooms = roomTiles.filter { it.data.type != RoomType.NORMAL }
            val roomTile = if (coloredRooms.size == 2) coloredRooms.find { it.data.type != RoomType.FAIRY }
            else coloredRooms.firstOrNull()
            roomTile?.getColor() ?: MapConfig.colorRoomDoor.value
        }
    }

    val roomTiles get() = roomTileIndices.mapNotNull { dungeonList[it] as? RoomTile }
    val roomTileIndices = buildList {
        val (row, column) = getGridPos()
        val rowEven = row and 1 == 0

        val neighbors = if (rowEven) listOfNotNull(
            (column - 1).takeIf { it >= 0 }?.let { row * 11 + it },
            (column + 1).takeIf { it <= 10 }?.let { row * 11 + it }
        )
        else listOfNotNull(
            (row - 1).takeIf { it >= 0 }?.let { it * 11 + column },
            (row + 1).takeIf { it <= 10 }?.let { it * 11 + column }
        )

        addAll(neighbors)
    }
}