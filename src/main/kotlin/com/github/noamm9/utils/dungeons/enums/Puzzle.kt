package com.github.noamm9.utils.dungeons.enums

import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.equalsOneOf

enum class Puzzle(val roomDataName: String, val tabName: String = roomDataName) {
    HIGHER_BLAZE("Higher Blaze", "Higher Or Lower"),
    LOWER_BLAZE("Lower Blaze", "Higher Or Lower"),
    TELEPORT_MAZE("Teleport Maze"),
    THREE_WEIRDOS("Three Weirdos"),
    CREEPER_BEAMS("Creeper Beams"),
    TIC_TAC_TOE("Tic Tac Toe"),
    WATER_BOARD("Water Board"),
    ICE_PATH("Ice Path"),
    ICE_FILL("Ice Fill"),
    BOULDER("Boulder"),
    QUIZ("Quiz"),
    UNKNOWN("???");

    companion object {
        fun fromName(name: String) = entries.find {
            name.equalsOneOf(it.roomDataName, it.tabName)
        }
    }

    var state = if (roomDataName == "???") RoomState.UNOPENED else RoomState.DISCOVERED

    val room get() = DungeonInfo.uniqueRooms[roomDataName]
}