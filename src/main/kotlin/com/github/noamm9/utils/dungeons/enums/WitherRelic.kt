package com.github.noamm9.utils.dungeons.enums

import com.github.noamm9.utils.MathUtils.vec
import net.minecraft.world.phys.Vec3
import java.awt.Color

enum class WitherRelic(
    val formalName: String,
    val colorCode: String,
    val cauldronPos: Vec3,
    val spawnPos: Vec3,
    val color: Color,
    val coords: Pair<Int, Int>,
) {
    RED("Corrupted Red Relic", "&c", vec(51, 7, 42), vec(20, 7, 59), Color(255, 0, 0, 40), 52 to 43),
    ORANGE("Corrupted Orange Relic", "&6", vec(57, 7, 42), vec(26, 7, 59), Color(255, 114, 0, 40), 58 to 43),
    GREEN("Corrupted Green Relic", "&a", vec(49.0, 7, 44), vec(20, 7, 94), Color(0, 255, 0, 40), 50 to 45),
    BLUE("Corrupted Blue Relic", "&b", vec(59, 7, 44), vec(91, 7, 94), Color(0, 138, 255, 40), 60 to 45),
    PURPLE("Corrupted Purple Relic", "&5", vec(54, 7, 41), vec(56, 9, 132), Color(129, 0, 111, 40), 55 to 42);

    val coloredName: String get() = "$colorCode${name.lowercase().replaceFirstChar { it.uppercase() }}"

    companion object {
        fun fromName(name: String): WitherRelic? = entries.find { it.formalName.equals(name, ignoreCase = true) }
    }
}