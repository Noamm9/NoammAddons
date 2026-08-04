package com.github.noamm9.utils.dungeons.map.core

import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import java.awt.Color

interface Tile {
    val x: Int
    val z: Int
    var state: RoomState
    fun getColor(): Color

    fun getGridPos(): Pair<Int, Int> {
        val halfRoom = DungeonScanner.roomSize shr 1
        val row = (z - DungeonScanner.startZ) / halfRoom
        val column = (x - DungeonScanner.startX) / halfRoom
        return row to column
    }
}