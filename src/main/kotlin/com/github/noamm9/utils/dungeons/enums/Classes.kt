package com.github.noamm9.utils.dungeons.enums

import java.awt.Color

enum class Classes(val color: Color) {
    Empty(Color(0, 0, 0)),
    Archer(Color(125, 0, 0)),
    Berserk(Color(205, 106, 0)),
    Healer(Color(123, 0, 123)),
    Mage(Color(0, 185, 185)),
    Tank(Color(0, 125, 0));

    companion object {
        fun getByName(name: String) = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Empty
        fun getColorCode(clazz: Classes): String {
            return when (clazz) {
                Archer -> "§4"
                Berserk -> "§6"
                Healer -> "§5"
                Mage -> "§3"
                Tank -> "§2"
                else -> "§7"
            }
        }
    }
}
