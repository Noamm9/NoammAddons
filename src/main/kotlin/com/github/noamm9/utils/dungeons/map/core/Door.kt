package com.github.noamm9.utils.dungeons.map.core

import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.dungeons.map.handlers.DungeonPathFinder
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import java.awt.Color

class Door(override val x: Int, override val z: Int, var type: DoorType): Tile {
    var opened = false
    override var state: RoomState = RoomState.UNDISCOVERED
    override val color: Color
        get() {
            if (state == RoomState.UNOPENED) return MapConfig.colorUnopenedDoor.value
            val color = when (this.type) {
                DoorType.BLOOD -> MapConfig.colorBloodDoor
                DoorType.ENTRANCE -> MapConfig.colorEntranceDoor
                DoorType.WITHER -> if (opened && state != RoomState.UNDISCOVERED) MapConfig.colorOpenWitherDoor
                else MapConfig.colorWitherDoor

                else -> MapConfig.colorRoomDoor
            }.value

            return if (state == RoomState.UNDISCOVERED && ! DungeonPathFinder.isFairy(this)) color.darker().darker() else color
        }

    val arrayPos = run {
        val step = DungeonScanner.roomSize shr 1
        val row = ((z - DungeonScanner.startZ) / step).coerceIn(0, 10)
        val column = ((x - DungeonScanner.startX) / step).coerceIn(0, 10)
        row to column
    }
}