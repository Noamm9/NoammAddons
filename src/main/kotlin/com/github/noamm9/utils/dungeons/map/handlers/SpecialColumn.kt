package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.core.RoomType.*
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils

/**
 * Based off dtMap by rice.who
 * Copyright (c) 2026, rice.who
 * under BSD-3 Clause license https://github.com/ricedotwho/dtMap/blob/main/LICENSE.txt
 * @link https://github.com/ricedotwho/dtMap/blob/main/src/main/kotlin/com/ricedotwho/dtmap/features/map/SpecialColumn.kt
 * Modified
 */
object SpecialColumn: ISelfInit {
    private var column = - 1
    private var roomsX = 0
    private var roomsZ = 0
    private var hasTrap = false

    // rows of the special column we can be sure about, based on the column right before it
    private var discoveredFullSpecialColumn = 0

    // only unopened rooms that are known to be 1x1 for a legit player
    private val known1x1s = mutableSetOf<Int>()
    private val opened1x1s = mutableMapOf<Int, RoomType>()

    override fun init() {
        EventBus.register<WorldChangeEvent> {
            column = - 1
            roomsX = 0
            roomsZ = 0
            hasTrap = false
            discoveredFullSpecialColumn = 0
            known1x1s.clear()
            opened1x1s.clear()
        }
    }

    fun update() {
        val floor = LocationUtils.dungeonFloorNumber ?: return
        roomsX = getRoomCount(MapUtils.startCorner.first)
        roomsZ = getRoomCount(MapUtils.startCorner.second)
        hasTrap = floor >= 3
        column = if (roomsX == 6 && ((floor in 5 .. 6 && roomsZ == 6) || (floor == 4 && roomsZ == 5))) 5 else - 1

        for (x in 0 until roomsX) for (z in 0 until roomsZ) {
            val index = getIndex(x, z)
            val tile = DungeonScanner.dungeonList[index] as? RoomTile ?: continue
            if (tile.data.type.equalsOneOf(BLOOD, ENTRANCE)) continue

            if (tile.state == RoomState.UNOPENED) {
                if (x == column || isSurrounded(x, z)) known1x1s.add(index)
                continue
            }

            if (tile.state == RoomState.UNDISCOVERED) continue
            if (tile.data.type.equalsOneOf(CHAMPION, TRAP, PUZZLE)) opened1x1s[index] = tile.data.type
        }

        if (known1x1s.size == getSpecialRoomCount(DungeonListener.maxPuzzleCount)) discoveredFullSpecialColumn = roomsZ
        else if (column != - 1) discoveredFullSpecialColumn = roomsZ - (0 until roomsZ).count { z ->
            val index = getIndex(column - 1, z)
            if (index in known1x1s) return@count false
            val tile = DungeonScanner.dungeonList[index]
            tile.state == RoomState.UNDISCOVERED || (tile.state == RoomState.UNOPENED && (tile as? RoomTile)?.data?.type != BLOOD)
        }
    }

    fun predict(index: Int): List<RoomType>? {
        if (index !in known1x1s) return null
        val puzzleCount = DungeonListener.maxPuzzleCount.takeIf { it > 0 } ?: return null
        if (getSeenDoors(index) != 1) return null

        val columnRooms = known1x1s.count(::isSpecial).let { if (column != - 1 && it == 0) 1 else it }
        // trap rooms only exist on F3 and above
        val openedTrap = ! hasTrap || TRAP in opened1x1s.values
        val openedChampion = CHAMPION in opened1x1s.values

        if (isSpecial(index)) {
            val specialSize = roomsZ - discoveredFullSpecialColumn + columnRooms
            return when {
                specialSize <= puzzleCount || known1x1s.size - columnRooms >= 2 -> listOf(PUZZLE)
                opened1x1s.any { (i, type) -> type == TRAP && ! isSpecial(i) } -> listOf(PUZZLE)
                specialSize == puzzleCount + 1 -> listOf(TRAP, PUZZLE)
                known1x1s.size - columnRooms == 1 -> listOf(TRAP, PUZZLE)
                openedChampion -> listOf(CHAMPION, TRAP)
                else -> listOf(CHAMPION, TRAP, PUZZLE)
            }
        }

        val totalPuzzles = columnRooms + opened1x1s.count { (i, type) -> type == PUZZLE && ! isSpecial(i) }
        if (totalPuzzles == puzzleCount) return when {
            openedTrap -> listOf(CHAMPION)
            openedChampion -> listOf(TRAP)
            else -> listOf(CHAMPION, TRAP)
        }

        // miniboss is generated firstb so if the special column has puzzles + 1 rooms the 1x1 on the map is the miniboss
        if (columnRooms == puzzleCount + 1) return listOf(CHAMPION)
        if (openedTrap && openedChampion) return listOf(PUZZLE)

        // only 1 unopened 1x1 is left and it cant be a puzzle because the check above didn't return
        if (opened1x1s.size == getSpecialRoomCount(puzzleCount) - 1) {
            if (! openedTrap) return listOf(TRAP)
            return listOf(CHAMPION)
        }

        return buildList {
            add(PUZZLE)
            if (! openedTrap) add(TRAP)
            if (! openedChampion) add(CHAMPION)
        }
    }

    private fun isSurrounded(x: Int, z: Int): Boolean {
        if (x != roomsX - 1 && x + 1 != column && ! isRevealed(x + 1, z)) return false
        if (x != 0 && ! isRevealed(x - 1, z)) return false
        if (z != roomsZ - 1 && ! isRevealed(x, z + 1)) return false
        if (z != 0 && ! isRevealed(x, z - 1)) return false
        return true
    }

    private fun isRevealed(x: Int, z: Int) = DungeonScanner.dungeonList[getIndex(x, z)].state != RoomState.UNDISCOVERED

    private fun getSeenDoors(index: Int): Int {
        val x = index % 11
        val z = index / 11

        return listOf(x - 1 to z, x + 1 to z, x to z - 1, x to z + 1).count { (dx, dz) ->
            if (dx !in 0 .. 10 || dz !in 0 .. 10) return@count false
            val door = DungeonScanner.dungeonList[dz * 11 + dx] as? DoorTile ?: return@count false
            door.state != RoomState.UNDISCOVERED
        }
    }

    private fun isSpecial(index: Int) = column != - 1 && (index % 11) / 2 == column
    private fun getSpecialRoomCount(puzzleCount: Int) = puzzleCount + if (hasTrap) 2 else 1
    private fun getIndex(x: Int, z: Int) = z * 2 * 11 + x * 2
    private fun getRoomCount(start: Int) = (128 - start * 2 + 4) / (MapUtils.mapRoomSize + 4)
}