package com.github.noamm9.utils.dungeons.map.core

enum class RoomType {
    BLOOD, FAIRY, RARE, CHAMPION, PUZZLE, TRAP, NORMAL, ENTRANCE;

    companion object {
        fun fromMapColor(color: Int) = when (color) {
            18 -> BLOOD
            82 -> FAIRY
            34 -> RARE
            74 -> CHAMPION
            66 -> PUZZLE
            62 -> TRAP
            63, 85 -> NORMAL
            30 -> ENTRANCE
            else -> null
        }
    }
}