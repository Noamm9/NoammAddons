package com.github.noamm9.utils.dungeons.enums

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.awt.Color

enum class WitherRelic(
    val formalName: String,
    val colorCode: String,
    val cauldronPos: Vec3,
    val color: Color,
    val spawnPos: BlockPos,
    val coords: Pair<Int, Int>,
) {
    RED("Corrupted Red Relic", "&c", Vec3(51.0, 7.0, 42.0), Color(255, 0, 0, 40), BlockPos(20, 7, 59), 52 to 43),
    ORANGE("Corrupted Orange Relic", "&6", Vec3(57.0, 7.0, 42.0), Color(255, 114, 0, 40), BlockPos(92, 7, 56), 58 to 43),
    GREEN("Corrupted Green Relic", "&a", Vec3(49.0, 7.0, 44.0), Color(0, 255, 0, 40), BlockPos(20, 7, 94), 50 to 45),
    BLUE("Corrupted Blue Relic", "&b", Vec3(59.0, 7.0, 44.0), Color(0, 138, 255, 40), BlockPos(91, 7, 94), 60 to 45),
    PURPLE("Corrupted Purple Relic", "&5", Vec3(54.0, 7.0, 41.0), Color(129, 0, 111, 40), BlockPos(56, 9, 132), 55 to 42);

    val coloredName: String get() = "$colorCode${name.lowercase().replaceFirstChar { it.uppercase() }}"

    companion object {
        fun fromName(name: String): WitherRelic? = entries.find { it.formalName.contains(name, ignoreCase = true) }
    }
}