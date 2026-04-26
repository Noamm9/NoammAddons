package com.github.noamm9.utils.dungeons.map.handlers

import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.Door
import com.github.noamm9.utils.dungeons.map.core.Room
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.dungeons.map.core.UniqueRoom
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf

// this is awful code please dont look
object DungeonPathFinder {
    private var splitsCache: Map<UniqueRoom, Set<UniqueRoom>>? = null
    private var bloodRushCache: List<UniqueRoom>? = null
    private var fairyRoom: UniqueRoom? = null
    private var nextRoomAfterFairy: UniqueRoom? = null
    private var roomBeforeFairy: UniqueRoom? = null

    fun clearCache() {
        splitsCache = null
        bloodRushCache = null
        fairyRoom = null
        nextRoomAfterFairy = null
        roomBeforeFairy = null
    }

    fun getConnectingDoorRooms(row: Int, column: Int): List<Room> {
        if (row !in 0 .. 10 || column !in 0 .. 10) return emptyList()
        val rooms = ArrayList<Room>(2)
        if (column and 1 == 0) {
            if (row > 0) (DungeonInfo.dungeonList[(row - 1) * 11 + column] as? Room)?.let { rooms.add(it) }
            if (row < 10) (DungeonInfo.dungeonList[(row + 1) * 11 + column] as? Room)?.let { rooms.add(it) }
        }
        else {
            if (column > 0) (DungeonInfo.dungeonList[row * 11 + column - 1] as? Room)?.let { rooms.add(it) }
            if (column < 10) (DungeonInfo.dungeonList[row * 11 + column + 1] as? Room)?.let { rooms.add(it) }
        }
        return rooms
    }

    fun isFairy(door: Door): Boolean {
        val currentRoom = ScanUtils.currentRoom ?: return false
        val lastRoom = ScanUtils.lastKnownRoom ?: return false

        getBloodRush()

        val fRoom = fairyRoom ?: return false
        val prevRoom = roomBeforeFairy ?: return false
        val nextRoom = nextRoomAfterFairy ?: return false

        if (! prevRoom.equalsOneOf(currentRoom, lastRoom)) return false

        val pos = door.arrayPos
        val row = pos.first
        val col = pos.second

        val r1: UniqueRoom?
        val r2: UniqueRoom?

        if (col and 1 == 0) {
            if (row !in 1 ..< 10) return false
            r1 = (DungeonInfo.dungeonList[(row - 1) * 11 + col] as? Room)?.uniqueRoom
            r2 = (DungeonInfo.dungeonList[(row + 1) * 11 + col] as? Room)?.uniqueRoom
        }
        else {
            if (col !in 1 ..< 10) return false
            r1 = (DungeonInfo.dungeonList[row * 11 + col - 1] as? Room)?.uniqueRoom
            r2 = (DungeonInfo.dungeonList[row * 11 + col + 1] as? Room)?.uniqueRoom
        }

        if (r1 == null || r2 == null) return false

        return (r1 == fRoom && r2 == nextRoom) || (r1 == nextRoom && r2 == fRoom)
    }

    fun getBloodRush(): List<UniqueRoom> {
        bloodRushCache?.let { return it }
        val graph = getSplits()
        val start = graph.keys.find { it.data.type == RoomType.ENTRANCE } ?: return emptyList()
        val target = graph.keys.find { it.data.type == RoomType.BLOOD } ?: return emptyList()

        val queue = ArrayDeque<UniqueRoom>()
        val previous = mutableMapOf<UniqueRoom, UniqueRoom?>()

        queue.add(start)
        previous[start] = null

        while (queue.isNotEmpty()) {
            val room = queue.removeFirst()
            if (room == target) break

            graph[room]?.forEach { neighbor ->
                if (neighbor !in previous) {
                    previous[neighbor] = room
                    queue.add(neighbor)
                }
            }
        }

        if (target !in previous) return emptyList()

        val path = mutableListOf<UniqueRoom>()
        var room: UniqueRoom? = target
        while (room != null) {
            path.add(room)
            room = previous[room]
        }

        val reversedPath = path.asReversed()
        bloodRushCache = reversedPath

        val fairyIndex = reversedPath.indexOfFirst { it.data.type == RoomType.FAIRY }
        if (fairyIndex > 0 && fairyIndex < reversedPath.size - 1) {
            roomBeforeFairy = reversedPath[fairyIndex - 1]
            fairyRoom = reversedPath[fairyIndex]
            nextRoomAfterFairy = reversedPath[fairyIndex + 1]
        }

        return reversedPath
    }

    private fun getSplits(): Map<UniqueRoom, Set<UniqueRoom>> {
        splitsCache?.let { return it }
        val graph = mutableMapOf<UniqueRoom, MutableSet<UniqueRoom>>()

        for (tile in DungeonInfo.dungeonList) {
            if (tile !is Door) continue
            val row = tile.arrayPos.first
            val col = tile.arrayPos.second

            val r1: UniqueRoom?
            val r2: UniqueRoom?

            if (col and 1 == 0) {
                if (row !in 1 ..< 10) continue
                r1 = (DungeonInfo.dungeonList[(row - 1) * 11 + col] as? Room)?.uniqueRoom
                r2 = (DungeonInfo.dungeonList[(row + 1) * 11 + col] as? Room)?.uniqueRoom
            }
            else {
                if (col !in 1 ..< 10) continue
                r1 = (DungeonInfo.dungeonList[row * 11 + col - 1] as? Room)?.uniqueRoom
                r2 = (DungeonInfo.dungeonList[row * 11 + col + 1] as? Room)?.uniqueRoom
            }

            if (r1 == null || r2 == null || r1 == r2) continue

            graph.getOrPut(r1) { mutableSetOf() }.add(r2)
            graph.getOrPut(r2) { mutableSetOf() }.add(r1)
        }

        splitsCache = graph
        return graph
    }
}