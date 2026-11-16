package com.github.noamm9.utils.dungeons.enums

import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.vec
import net.minecraft.world.phys.Vec3
import java.awt.Color

enum class WitherRelic(
    val formalName: String,
    val clazz: DungeonClass,
    val cauldronPos: Vec3,
    val spawnPos: Vec3,
    val coords: Pair<Int, Int>,
) {
    RED("Corrupted Red Relic", DungeonClass.Archer, vec(51, 7, 42), vec(20, 7, 59), 52 to 43),
    ORANGE("Corrupted Orange Relic", DungeonClass.Berserk, vec(57, 7, 42), vec(26, 7, 59), 58 to 43),
    GREEN("Corrupted Green Relic", DungeonClass.Tank, vec(49.0, 7, 44), vec(20, 7, 94), 50 to 45),
    BLUE("Corrupted Blue Relic", DungeonClass.Mage, vec(59, 7, 44), vec(91, 7, 94), 60 to 45),
    PURPLE("Corrupted Purple Relic", DungeonClass.Healer, vec(54, 7, 41), vec(56, 9, 132), 55 to 42);

    val coloredName: String get() = "$colorCode${name.lowercase().replaceFirstChar { it.uppercase() }}"
    val colorCode: String get() = clazz.code
    val color: Color get() = clazz.color.withAlpha(40)

    companion object {
        fun fromName(name: String): WitherRelic? = entries.find { it.formalName.equals(name, ignoreCase = true) }
    }
}