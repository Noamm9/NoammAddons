package com.github.noamm9.utils.dungeons.map.core

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

enum class DoorType(val source: Block) {
    BLOOD(Blocks.RED_TERRACOTTA),
    WITHER(Blocks.COAL_BLOCK),
    NORMAL(Blocks.AIR),
    ENTRANCE(Blocks.INFESTED_CHISELED_STONE_BRICKS);

    var keys = 0

    companion object {
        fun reset() = entries.forEach { it.keys = 0 }

        fun fromMapColor(color: Int) = when (color) {
            18 -> BLOOD
            30 -> ENTRANCE
            119 -> WITHER
            74, 82, 66, 62, 85, 63 -> NORMAL
            else -> null
        }
    }
}