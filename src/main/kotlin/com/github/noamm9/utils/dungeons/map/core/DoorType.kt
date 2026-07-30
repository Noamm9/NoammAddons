package com.github.noamm9.utils.dungeons.map.core

enum class DoorType {
    BLOOD, WITHER, NORMAL, ENTRANCE;

    companion object {
        fun fromMapColor(color: Int) = when (color) {
            18 -> BLOOD
            30 -> ENTRANCE
            119 -> WITHER
            74, 82, 66, 62, 85, 63 -> NORMAL
            else -> null
        }
    }
}